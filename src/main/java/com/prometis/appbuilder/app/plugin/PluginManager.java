package com.prometis.appbuilder.app.plugin;

import com.prometis.appbuilder.app.node.code.FunctionType;
import com.prometis.appbuilder.app.plugin.runtime.LocalPluginState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 플러그인 등록, 리로드, 시작, 중지(와 삭제, pluginPath 관리).
 *
 * <p>모든 변경은 먼저 DB(Plugin, PluginFile, PluginPath)에 기록하고, 커밋한 뒤 이 서버에 바로 반영한다.
 * 서버가 여러 대면 나머지 서버는 {@link PluginSynchronizer} 가 DB 를 보고 따라온다. 그래서 jar 를 서버마다 따로 올릴 필요가 없다.</p>
 *
 * <p>핫리로드: jar 를 다시 올리거나 리로드하면 revision 이 올라가고, 각 서버는 애플리케이션을 재시작하지 않고
 * 기존 플러그인을 내린 뒤 새 jar 를 로드해서 시작한다.</p>
 */
@Service
@RequiredArgsConstructor
public class PluginManager {
    private static final Pattern PATH_PATTERN = Pattern.compile(PluginPath.PATH_PATTERN);

    private final PluginRepository pluginRepository;
    private final PluginFileRepository pluginFileRepository;
    private final PluginPathRepository pluginPathRepository;
    private final PluginServerStateRepository serverStateRepository;
    private final PluginSynchronizer synchronizer;
    private final PluginProperties properties;
    private final TransactionTemplate transactionTemplate;

    public PluginProperties policy() {
        return properties;
    }

    public List<PluginView> getPlugins(String applicationId) {
        List<Plugin> plugins = pluginRepository.findByApplicationIdOrderByPluginIdAsc(applicationId);

        Map<Long, List<PluginView.PathView>> paths = new LinkedHashMap<>();
        for(PluginPath path : pluginPathRepository.findAllWithPlugin(applicationId)) {
            paths.computeIfAbsent(path.getPlugin().getId(), k -> new ArrayList<>())
                    .add(PluginView.PathView.of(path, path.getPlugin().getPluginId()));
        }

        Map<String, List<PluginServerState>> states = serverStateRepository.findByApplicationIdOrderByPluginIdAscServerIdAsc(applicationId)
                .stream()
                .collect(Collectors.groupingBy(PluginServerState::getPluginId, LinkedHashMap::new, Collectors.toList()));

        return plugins.stream()
                .map(plugin -> viewOf(plugin, paths.getOrDefault(plugin.getId(), List.of()), states.getOrDefault(plugin.getPluginId(), List.of())))
                .toList();
    }

    public PluginView getPlugin(String applicationId, String pluginId) {
        return getPlugins(applicationId).stream()
                .filter(plugin -> plugin.pluginId().equals(pluginId))
                .findFirst()
                .orElseThrow(() -> notFound(pluginId));
    }

    /**
     * 애플리케이션의 모든 pluginPath (워크플로우 PLUGIN 노드에서 고르는 목록).
     */
    public List<PluginView.PathView> getPaths(String applicationId) {
        return pluginPathRepository.findAllWithPlugin(applicationId).stream()
                .map(path -> PluginView.PathView.of(path, path.getPlugin().getPluginId()))
                .toList();
    }

    /**
     * 플러그인 jar 를 등록한다. MANIFEST 의 Plugin-Id 가 이미 있으면 그 플러그인의 jar 를 새 버전으로 바꾸고 다시 로드한다(핫리로드).
     *
     * @param expectedPluginId 특정 플러그인의 jar 를 교체하려는 것이면 그 플러그인ID (다른 플러그인을 덮어쓰지 않도록 확인한다). 신규 등록이면 null
     */
    public PluginView register(String applicationId, MultipartFile file, String expectedPluginId) {
        if(file == null || file.isEmpty()) {
            throw new PluginException("업로드할 jar 파일을 선택해주세요.");
        }

        String originalFilename = file.getOriginalFilename() == null ? "plugin.jar" : file.getOriginalFilename();
        if(!originalFilename.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            throw new PluginException("플러그인은 jar 파일만 등록할 수 있습니다. [" + originalFilename + "]");
        }
        if(file.getSize() > properties.maxFileSize().toBytes()) {
            throw new PluginException("플러그인 jar 는 " + properties.maxFileSize().toMegabytes() + "MB 를 넘을 수 없습니다.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        }
        catch (IOException e) {
            throw new PluginException("업로드한 파일을 읽을 수 없습니다.", e);
        }

        PluginJarInfo info = PluginJarInspector.inspect(content);
        if(expectedPluginId != null && !expectedPluginId.isBlank() && !expectedPluginId.equals(info.pluginId())) {
            throw new PluginException("jar 의 Plugin-Id [" + info.pluginId() + "] 가 교체하려는 플러그인 [" + expectedPluginId + "] 과 다릅니다.");
        }

        String checksum = sha256(content);
        String fileName = originalFilename.length() > 200 ? originalFilename.substring(originalFilename.length() - 200) : originalFilename;

        Plugin saved = transactionTemplate.execute(status -> {
            Plugin plugin = pluginRepository.findByApplicationIdAndPluginId(applicationId, info.pluginId()).orElse(null);

            if(plugin == null) {
                long count = pluginRepository.countByApplicationId(applicationId);
                if(count >= properties.maxCount()) {
                    throw new PluginException("플러그인은 애플리케이션당 최대 " + properties.maxCount() + "개까지 등록할 수 있습니다.");
                }

                plugin = pluginRepository.save(Plugin.builder()
                        .applicationId(applicationId)
                        .pluginId(info.pluginId())
                        .version(info.version())
                        .provider(info.provider())
                        .description(info.description())
                        .originalFilename(fileName)
                        .fileSize((long) content.length)
                        .checksum(checksum)
                        .revision(1L)
                        .status(PluginStatus.STARTED)
                        .build());

                pluginFileRepository.save(PluginFile.builder()
                        .pluginSeq(plugin.getId())
                        .content(content)
                        .build());
            }
            else {
                plugin.replaceJar(info, fileName, content.length, checksum);
                pluginRepository.save(plugin);

                Long pluginSeq = plugin.getId();
                PluginFile pluginFile = pluginFileRepository.findByPluginSeq(pluginSeq)
                        .orElseGet(() -> PluginFile.builder().pluginSeq(pluginSeq).build());
                pluginFile.replace(content);
                pluginFileRepository.save(pluginFile);
            }
            return plugin;
        });

        synchronizer.sync(saved);
        return getPlugin(applicationId, saved.getPluginId());
    }

    /**
     * DB 에 있는 jar 로 플러그인을 다시 로드한다. 모든 서버가 내렸다가 다시 로드한다.
     */
    public PluginView reload(String applicationId, String pluginId) {
        Plugin saved = transactionTemplate.execute(status -> {
            Plugin plugin = find(applicationId, pluginId);
            plugin.increaseRevision();
            return pluginRepository.save(plugin);
        });

        synchronizer.sync(saved);
        return getPlugin(applicationId, pluginId);
    }

    public PluginView start(String applicationId, String pluginId) {
        return changeStatus(applicationId, pluginId, PluginStatus.STARTED);
    }

    public PluginView stop(String applicationId, String pluginId) {
        return changeStatus(applicationId, pluginId, PluginStatus.STOPPED);
    }

    /**
     * 플러그인을 지운다. 워크플로우가 이 플러그인의 pluginPath 를 쓰고 있으면 지우지 않는다.
     */
    public void delete(String applicationId, String pluginId) {
        transactionTemplate.executeWithoutResult(status -> {
            Plugin plugin = find(applicationId, pluginId);

            List<String> paths = pluginPathRepository.findByPluginSeq(plugin.getId()).stream().map(PluginPath::getPluginPath).toList();
            requireUnused(applicationId, paths, "플러그인을 삭제할 수 없습니다");

            pluginPathRepository.deleteByPluginSeq(plugin.getId());
            pluginFileRepository.deleteByPluginSeq(plugin.getId());
            serverStateRepository.deleteByPlugin(applicationId, pluginId);
            pluginRepository.deleteById(plugin.getId());
        });

        synchronizer.remove(applicationId, pluginId);
    }

    /**
     * 플러그인의 pluginPath 를 통째로 바꾼다. pluginPath 는 애플리케이션 안에서 유일해야 하고,
     * 워크플로우가 쓰고 있는 pluginPath 는 뺄 수 없다.
     */
    public PluginView savePaths(String applicationId, String pluginId, List<PluginView.PathRequest> requests) {
        List<PluginView.PathRequest> items = requests == null ? List.of() : requests;

        transactionTemplate.executeWithoutResult(status -> {
            Plugin plugin = find(applicationId, pluginId);

            Set<String> requested = new HashSet<>();
            for(PluginView.PathRequest item : items) {
                String path = item.pluginPath() == null ? "" : item.pluginPath().trim();
                if(!PATH_PATTERN.matcher(path).matches()) {
                    throw new PluginException("pluginPath 는 영문/숫자로 시작하고 영문/숫자/ _ . - / 만 쓸 수 있습니다(100자 이내). [" + path + "]");
                }
                if(!requested.add(path)) {
                    throw new PluginException("pluginPath 가 중복됩니다. [" + path + "]");
                }
                if(item.displayName() != null && item.displayName().length() > 200) {
                    throw new PluginException("표시명은 200자 이내로 입력해주세요. [" + path + "]");
                }
                if(item.description() != null && item.description().length() > 500) {
                    throw new PluginException("설명은 500자 이내로 입력해주세요. [" + path + "]");
                }
            }

            for(PluginPath other : pluginPathRepository.findAllWithPlugin(applicationId)) {
                if(!other.getPlugin().getId().equals(plugin.getId()) && requested.contains(other.getPluginPath())) {
                    throw new PluginException("pluginPath [" + other.getPluginPath() + "] 는 다른 플러그인 ["
                            + other.getPlugin().getPluginId() + "] 에서 이미 쓰고 있습니다.");
                }
            }

            List<String> removed = pluginPathRepository.findByPluginSeq(plugin.getId()).stream()
                    .map(PluginPath::getPluginPath)
                    .filter(path -> !requested.contains(path))
                    .toList();
            requireUnused(applicationId, removed, "pluginPath 를 뺄 수 없습니다");

            pluginPathRepository.deleteByPluginSeq(plugin.getId());

            Plugin managed = find(applicationId, pluginId);
            int order = 0;
            for(PluginView.PathRequest item : items) {
                pluginPathRepository.save(PluginPath.builder()
                        .applicationId(applicationId)
                        .plugin(managed)
                        .pluginPath(item.pluginPath().trim())
                        .displayName(blankToNull(item.displayName()))
                        .description(blankToNull(item.description()))
                        .orderNum(order++)
                        .build());
            }
        });

        return getPlugin(applicationId, pluginId);
    }

    private PluginView changeStatus(String applicationId, String pluginId, PluginStatus pluginStatus) {
        Plugin saved = transactionTemplate.execute(status -> {
            Plugin plugin = find(applicationId, pluginId);
            plugin.changeStatus(pluginStatus);
            return pluginRepository.save(plugin);
        });

        synchronizer.sync(saved);
        return getPlugin(applicationId, pluginId);
    }

    private void requireUnused(String applicationId, Collection<String> paths, String action) {
        if(paths.isEmpty()) {
            return;
        }

        List<Object[]> usages = pluginPathRepository.findWorkflowUsages(applicationId, FunctionType.PLUGIN, paths);
        if(!usages.isEmpty()) {
            Set<String> described = new TreeSet<>();
            for(Object[] usage : usages) {
                described.add(usage[0] + "(" + usage[1] + ")");
            }
            throw new PluginException(action + ". 워크플로우에서 쓰고 있습니다: " + String.join(", ", described));
        }
    }

    private Plugin find(String applicationId, String pluginId) {
        return pluginRepository.findByApplicationIdAndPluginId(applicationId, pluginId).orElseThrow(() -> notFound(pluginId));
    }

    private PluginView viewOf(Plugin plugin, List<PluginView.PathView> paths, List<PluginServerState> states) {
        String currentServer = synchronizer.getServerId();
        String expectedState = plugin.isStarted() ? "STARTED" : null;

        List<PluginView.ServerView> servers = states.stream()
                .map(state -> {
                    boolean revisionSynced = plugin.getRevision().equals(state.getRevision());
                    boolean stateSynced = (expectedState != null)
                            ? expectedState.equals(state.getState())
                            : !"STARTED".equals(state.getState()) && !LocalPluginState.FAILED.equals(state.getState());
                    return new PluginView.ServerView(
                            state.getServerId(),
                            currentServer.equals(state.getServerId()),
                            state.getRevision(),
                            state.getState(),
                            state.getMessage(),
                            revisionSynced && stateSynced,
                            state.getUpdatedAt());
                })
                .toList();

        return new PluginView(
                plugin.getId(),
                plugin.getPluginId(),
                plugin.getVersion(),
                plugin.getProvider(),
                plugin.getDescription(),
                plugin.getOriginalFilename(),
                plugin.getFileSize(),
                plugin.getChecksum(),
                plugin.getRevision(),
                plugin.getStatus(),
                plugin.getCreatedAt(),
                plugin.getUpdatedAt(),
                paths,
                servers);
    }

    private static PluginException notFound(String pluginId) {
        return new PluginException("등록되지 않은 플러그인입니다. [" + pluginId + "]");
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
