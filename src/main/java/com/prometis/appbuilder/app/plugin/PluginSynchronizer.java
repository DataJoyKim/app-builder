package com.prometis.appbuilder.app.plugin;

import com.prometis.appbuilder.app.plugin.runtime.LocalPluginState;
import com.prometis.appbuilder.app.plugin.runtime.PluginRuntime;
import com.prometis.appbuilder.app.plugin.runtime.PluginRuntime.DesiredPlugin;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * DB(Plugin)를 원본으로 이 서버의 플러그인을 맞춘다.
 *
 * <p>서버가 여러 대일 때 콘솔 요청은 그중 한 대에만 닿는다. 그 서버는 저장 직후 바로 맞추고({@link #sync(Plugin)}),
 * 나머지 서버는 기동할 때와 platform.plugin.sync-interval 주기마다 DB 전체를 다시 읽어({@link #syncAll()}) 같은 상태로 따라온다.
 * 실행 요청이 먼저 닿으면 PluginService 가 그 플러그인만 즉시 맞춘다. 각 서버가 맞춘 결과는 PluginServerState 에 남겨 콘솔에서 서버별로 확인한다.</p>
 */
@Slf4j
@Component
public class PluginSynchronizer {
    private final PluginRepository pluginRepository;
    private final PluginFileRepository pluginFileRepository;
    private final PluginServerStateRepository serverStateRepository;
    private final PluginRuntime runtime;
    private final PluginProperties properties;

    @Getter
    private final String serverId;

    // 마지막으로 기록한 상태. 바뀌었을 때만 DB 에 쓴다
    private final Map<String, LocalPluginState> recorded = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    public PluginSynchronizer(PluginRepository pluginRepository,
                              PluginFileRepository pluginFileRepository,
                              PluginServerStateRepository serverStateRepository,
                              PluginRuntime runtime,
                              PluginProperties properties,
                              Environment environment) {
        this.pluginRepository = pluginRepository;
        this.pluginFileRepository = pluginFileRepository;
        this.serverStateRepository = serverStateRepository;
        this.runtime = runtime;
        this.properties = properties;
        this.serverId = resolveServerId(properties, environment);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        syncAllQuietly();

        long interval = properties.syncInterval().toMillis();
        if(interval > 0) {
            scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "plugin-sync");
                thread.setDaemon(true);
                return thread;
            });
            scheduler.scheduleWithFixedDelay(this::syncAllQuietly, interval, interval, TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    public void stopScheduler() {
        if(scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /**
     * DB 의 플러그인 전체로 이 서버를 맞추고, DB 에 없는 플러그인은 내린다.
     */
    public synchronized void syncAll() {
        List<Plugin> plugins = pluginRepository.findAll();
        List<DesiredPlugin> desiredPlugins = plugins.stream().map(PluginSynchronizer::desiredOf).toList();

        for(Plugin plugin : plugins) {
            apply(plugin);
        }

        runtime.retainOnly(desiredPlugins);
        recorded.keySet().removeIf(key -> plugins.stream().noneMatch(plugin -> key.equals(keyOf(plugin.getApplicationId(), plugin.getPluginId()))));
    }

    /**
     * 플러그인 하나를 이 서버에 맞춘다 (콘솔에서 바꾼 직후, 또는 실행 요청에 아직 반영되지 않았을 때).
     */
    public synchronized LocalPluginState sync(Plugin plugin) {
        return apply(plugin);
    }

    /**
     * DB 에서 지운 플러그인을 이 서버에서 내린다. 다른 서버는 다음 {@link #syncAll()} 에서 내린다.
     */
    public synchronized void remove(String applicationId, String pluginId) {
        runtime.remove(applicationId, pluginId);
        recorded.remove(keyOf(applicationId, pluginId));
    }

    private void syncAllQuietly() {
        try {
            syncAll();
        }
        catch (Exception e) {
            log.error("Plugin - 플러그인 동기화 실패", e);
        }
    }

    private LocalPluginState apply(Plugin plugin) {
        LocalPluginState state;
        try {
            state = runtime.apply(desiredOf(plugin), () -> pluginFileRepository.findContent(plugin.getId()).orElse(null));
        }
        catch (Exception e) {
            log.error("Plugin - [{}/{}] 동기화 실패", plugin.getApplicationId(), plugin.getPluginId(), e);
            state = new LocalPluginState(plugin.getRevision(), LocalPluginState.FAILED, e.getMessage());
        }
        record(plugin.getApplicationId(), plugin.getPluginId(), state);
        return state;
    }

    private void record(String applicationId, String pluginId, LocalPluginState state) {
        String key = keyOf(applicationId, pluginId);
        if(state.equals(recorded.get(key))) {
            return;
        }

        try {
            PluginServerState row = serverStateRepository.findByServerIdAndApplicationIdAndPluginId(serverId, applicationId, pluginId)
                    .orElseGet(() -> PluginServerState.builder()
                            .serverId(serverId)
                            .applicationId(applicationId)
                            .pluginId(pluginId)
                            .build());
            row.update(state.revision(), state.state(), state.message());
            serverStateRepository.save(row);
            recorded.put(key, state);
        }
        catch (Exception e) {
            // 상태 기록은 콘솔 표시용이라 실패해도 플러그인 동작에는 영향이 없다. 다음 동기화 때 다시 쓴다
            log.warn("Plugin - [{}/{}] 서버 상태 기록 실패: {}", applicationId, pluginId, e.getMessage());
        }
    }

    private static DesiredPlugin desiredOf(Plugin plugin) {
        return new DesiredPlugin(plugin.getApplicationId(), plugin.getPluginId(), plugin.getRevision(), plugin.isStarted());
    }

    private static String keyOf(String applicationId, String pluginId) {
        return applicationId + "\n" + pluginId;
    }

    private static String resolveServerId(PluginProperties properties, Environment environment) {
        if(properties.serverId() != null && !properties.serverId().isBlank()) {
            return properties.serverId().trim();
        }

        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        }
        catch (Exception e) {
            host = "unknown-host";
        }
        return host + ":" + environment.getProperty("server.port", "8080");
    }
}
