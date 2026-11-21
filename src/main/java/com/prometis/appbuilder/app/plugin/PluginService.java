package com.prometis.appbuilder.app.plugin;

import com.prometis.appbuilder.app.plugin.runtime.LocalPluginState;
import com.prometis.appbuilder.app.plugin.runtime.PluginRuntime;
import com.prometis.appbuilder.app.plugin.runtime.PluginUnavailableException;
import com.prometis.plugin.api.PluginRequest;
import com.prometis.plugin.api.PluginResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * 등록된 플러그인을 실행한다 (워크플로우 PLUGIN 노드 → PluginExecutor → 여기).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PluginService {
    private final PluginPathRepository pluginPathRepository;
    private final PluginRuntime runtime;
    private final PluginSynchronizer synchronizer;

    /**
     * @param applicationId 실행하는 애플리케이션. 이 애플리케이션에 등록된 플러그인/pluginPath 만 쓸 수 있다
     * @param pluginPath    실행할 pluginPath. 이 값으로 플러그인을 찾고, 플러그인에도 그대로 넘겨 라우팅에 쓰게 한다
     * @param params        플러그인으로 넘길 요청정보
     */
    public PluginResult execute(String applicationId, String pluginPath, PluginRequest params) {
        Optional<PluginPath> found = pluginPathRepository.findWithPlugin(applicationId, pluginPath);
        if(found.isEmpty()) {
            return PluginResult.failure("등록되지 않은 pluginPath 입니다. [" + pluginPath + "]");
        }

        Plugin plugin = found.get().getPlugin();
        if(!plugin.isStarted()) {
            return PluginResult.failure("중지된 플러그인입니다. [" + plugin.getPluginId() + "]");
        }

        // 다른 서버에서 방금 올리거나 리로드해서 이 서버가 아직 주기 동기화를 못 했으면 지금 맞춘다
        LocalPluginState local = runtime.state(applicationId, plugin.getPluginId());
        if(!Objects.equals(local.revision(), plugin.getRevision()) || !local.isStarted() && !LocalPluginState.FAILED.equals(local.state())) {
            synchronizer.sync(plugin);
        }

        PluginRequest request = (params == null) ? new PluginRequest(null) : params;
        HostPluginContext context = new HostPluginContext(applicationId);

        try {
            PluginResult result = runtime.execute(applicationId, plugin.getPluginId(),
                    workflowNodePlugin -> workflowNodePlugin.execute(pluginPath, request, context));

            return (result == null) ? PluginResult.failure("플러그인이 결과를 돌려주지 않았습니다. [" + pluginPath + "]") : result;
        }
        catch (PluginUnavailableException e) {
            return PluginResult.failure(e.getMessage());
        }
        catch (RuntimeException | LinkageError e) {
            // 플러그인 코드의 예외나 클래스 누락(NoClassDefFoundError 등)이 워크플로우 전체를 깨지 않도록 실패 결과로 바꾼다
            log.error("Plugin - [{}/{}] pluginPath [{}] 실행 오류", applicationId, plugin.getPluginId(), pluginPath, e);
            return PluginResult.failure("플러그인 실행 중 오류가 발생했습니다. [" + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : ": " + e.getMessage()) + "]");
        }
    }
}
