package com.prometis.appbuilder.app.plugin.runtime;

import com.prometis.plugin.api.WorkflowNodePlugin;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 이 서버(인스턴스) 안의 pf4j 플러그인을 애플리케이션별로 들고 있는 곳.
 *
 * <p>무엇을 어떤 상태로 들고 있어야 하는지는 DB(Plugin)가 정하고, 이 클래스는 그 목표 상태({@link DesiredPlugin})에 맞춰
 * jar 를 {root}/{applicationId}/plugins 에 쓰고 load / start / stop / unload 만 한다. 서버가 여러 대면 서버마다 이 클래스가
 * 같은 DB 를 보고 각자 맞추므로, 콘솔에서 한 번 올린 플러그인이 모든 서버에 퍼진다.</p>
 *
 * <p>jar 파일 이름은 {pluginId}-{revision}.jar 이다. Windows 에서는 로드했던 jar 가 잠겨 같은 이름으로 덮어쓸 수 없으므로,
 * 새 revision 은 새 파일로 쓰고 지난 파일은 지울 수 있을 때 지운다(핫리로드).</p>
 *
 * <p>애플리케이션마다 읽기/쓰기 잠금을 둔다. 플러그인 실행은 읽기 잠금이라 동시에 돌고, 리로드/중지는 쓰기 잠금이라
 * 실행 중인 호출이 끝난 뒤 클래스로더를 닫는다.</p>
 */
@Slf4j
public class PluginRuntime {
    private static final String PLUGINS_DIR = "plugins";
    private static final String JAR_SUFFIX = ".jar";

    private final Path rootPath;
    private final Map<String, AppPlugins> apps = new ConcurrentHashMap<>();

    /**
     * @param applicationId 플러그인이 속한 애플리케이션
     * @param pluginId      MANIFEST Plugin-Id
     * @param revision      DB 의 revision. 로드한 revision 과 다르면 jar 를 다시 받아 로드한다
     * @param started       시작 상태여야 하면 true, 중지 상태여야 하면 false
     */
    public record DesiredPlugin(String applicationId, String pluginId, long revision, boolean started) {
    }

    public PluginRuntime(String rootPath) {
        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
    }

    public Path pluginsDirectory(String applicationId) {
        Path dir = rootPath.resolve(applicationId).resolve(PLUGINS_DIR).normalize();
        if(applicationId == null || applicationId.isBlank() || !dir.startsWith(rootPath) || !dir.getParent().getParent().equals(rootPath)) {
            throw new IllegalArgumentException("플러그인 경로로 쓸 수 없는 애플리케이션ID 입니다. [" + applicationId + "]");
        }
        return dir;
    }

    /**
     * 플러그인 하나를 목표 상태로 맞춘다.
     *
     * @param jarSupplier revision 이 바뀌어 jar 를 새로 써야 할 때만 불린다 (DB 에서 LOB 를 읽는 비용을 아끼려고)
     * @return 맞춘 뒤 이 서버의 상태
     */
    public LocalPluginState apply(DesiredPlugin desired, Supplier<byte[]> jarSupplier) {
        AppPlugins app = app(desired.applicationId());
        app.lock.writeLock().lock();
        try {
            String pluginId = desired.pluginId();
            Long attempted = app.attemptedRevisions.get(pluginId);

            // 같은 revision 으로 이미 로드했거나, 로드에 실패했다면 다시 시도하지 않는다 (리로드하면 revision 이 올라 다시 시도한다)
            if(attempted == null || attempted != desired.revision()) {
                load(app, desired, jarSupplier);
            }

            PluginWrapper wrapper = app.manager.getPlugin(pluginId);
            if(wrapper != null) {
                PluginState state = wrapper.getPluginState();
                if(desired.started() && (state == PluginState.RESOLVED || state == PluginState.STOPPED)) {
                    start(app, wrapper, desired.revision());
                }
                else if(!desired.started() && state == PluginState.STARTED) {
                    stop(app, pluginId, desired.revision());
                }
            }

            return app.states.getOrDefault(pluginId, LocalPluginState.notLoaded());
        }
        finally {
            app.lock.writeLock().unlock();
        }
    }

    /**
     * 플러그인을 내리고 jar 파일을 지운다 (DB 에서 삭제된 플러그인).
     */
    public void remove(String applicationId, String pluginId) {
        AppPlugins app = apps.get(applicationId);
        if(app == null) {
            return;
        }

        app.lock.writeLock().lock();
        try {
            unload(app, pluginId);
            app.attemptedRevisions.remove(pluginId);
            app.states.remove(pluginId);
            deleteJars(app, pluginId, null);
        }
        finally {
            app.lock.writeLock().unlock();
        }
    }

    /**
     * 목록에 없는 플러그인을 모두 내리고, 각 애플리케이션 플러그인 경로에서 지금 쓰지 않는 jar 를 지운다.
     *
     * @param desiredPlugins DB 에 있는 플러그인 전체
     */
    public void retainOnly(Collection<DesiredPlugin> desiredPlugins) {
        Map<String, Map<String, Long>> keep = new HashMap<>();
        for(DesiredPlugin desired : desiredPlugins) {
            keep.computeIfAbsent(desired.applicationId(), k -> new HashMap<>()).put(desired.pluginId(), desired.revision());
        }

        for(AppPlugins app : apps.values()) {
            Map<String, Long> keepPlugins = keep.getOrDefault(app.applicationId, Map.of());
            for(String pluginId : new ArrayList<>(app.attemptedRevisions.keySet())) {
                if(!keepPlugins.containsKey(pluginId)) {
                    remove(app.applicationId, pluginId);
                }
            }
        }

        // 서버가 내려가 있는 동안 지워지거나 교체된 플러그인의 jar 파일 정리 (로드되어 잠긴 파일은 다음에 다시 시도한다)
        keep.forEach((applicationId, plugins) -> {
            Set<String> fileNames = new HashSet<>();
            plugins.forEach((pluginId, revision) -> fileNames.add(jarFileName(pluginId, revision)));
            cleanupDirectory(pluginsDirectory(applicationId), fileNames);
        });
    }

    public LocalPluginState state(String applicationId, String pluginId) {
        AppPlugins app = apps.get(applicationId);
        if(app == null) {
            return LocalPluginState.notLoaded();
        }
        return app.states.getOrDefault(pluginId, LocalPluginState.notLoaded());
    }

    /**
     * 시작된 플러그인의 WorkflowNodePlugin 구현체로 작업을 실행한다. 실행하는 동안 그 애플리케이션의 리로드/중지는 기다린다.
     * 스레드 컨텍스트 클래스로더를 플러그인 클래스로더로 바꿔서, 플러그인이 넣어 온 라이브러리가 자기 리소스를 찾을 수 있게 한다.
     *
     * @throws PluginUnavailableException 이 서버에 시작된 플러그인이 없거나 구현체가 없을 때
     */
    public <R> R execute(String applicationId, String pluginId, Function<WorkflowNodePlugin, R> work) {
        AppPlugins app = apps.get(applicationId);
        if(app == null) {
            throw new PluginUnavailableException("플러그인이 이 서버에 로드되어 있지 않습니다. [" + pluginId + "]");
        }

        app.lock.readLock().lock();
        try {
            PluginWrapper wrapper = app.manager.getPlugin(pluginId);
            if(wrapper == null) {
                throw new PluginUnavailableException("플러그인이 이 서버에 로드되어 있지 않습니다. [" + pluginId + "]");
            }
            if(wrapper.getPluginState() != PluginState.STARTED) {
                throw new PluginUnavailableException("플러그인이 시작되어 있지 않습니다. [" + pluginId + ", " + wrapper.getPluginState() + "]");
            }

            List<WorkflowNodePlugin> extensions = app.manager.getExtensions(WorkflowNodePlugin.class, pluginId);
            if(extensions.isEmpty()) {
                throw new PluginUnavailableException("플러그인에 WorkflowNodePlugin 구현체(@Extension)가 없습니다. [" + pluginId + "]");
            }
            if(extensions.size() > 1) {
                log.warn("Plugin - [{}/{}] WorkflowNodePlugin 구현체가 {}개라 첫 번째({})를 씁니다.",
                        applicationId, pluginId, extensions.size(), extensions.get(0).getClass().getName());
            }

            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            thread.setContextClassLoader(wrapper.getPluginClassLoader());
            try {
                return work.apply(extensions.get(0));
            }
            finally {
                thread.setContextClassLoader(previous);
            }
        }
        finally {
            app.lock.readLock().unlock();
        }
    }

    @PreDestroy
    public void shutdown() {
        for(AppPlugins app : apps.values()) {
            app.lock.writeLock().lock();
            try {
                app.manager.stopPlugins();
                app.manager.unloadPlugins();
            }
            catch (Exception e) {
                log.warn("Plugin - [{}] 플러그인 종료 중 오류", app.applicationId, e);
            }
            finally {
                app.lock.writeLock().unlock();
            }
        }
        apps.clear();
    }

    private void load(AppPlugins app, DesiredPlugin desired, Supplier<byte[]> jarSupplier) {
        String pluginId = desired.pluginId();
        long revision = desired.revision();

        unload(app, pluginId);
        app.attemptedRevisions.put(pluginId, revision);

        Path jar = app.directory.resolve(jarFileName(pluginId, revision));
        try {
            byte[] content = jarSupplier.get();
            if(content == null || content.length == 0) {
                throw new IllegalStateException("DB 에 플러그인 jar 본문이 없습니다.");
            }
            writeJar(jar, content);

            String loadedId = app.manager.loadPlugin(jar);
            if(!pluginId.equals(loadedId)) {
                app.manager.unloadPlugin(loadedId);
                throw new IllegalStateException("jar 의 Plugin-Id [" + loadedId + "] 가 등록된 플러그인ID 와 다릅니다.");
            }

            app.states.put(pluginId, stateOf(app, pluginId, revision, null));
            deleteJars(app, pluginId, jar.getFileName().toString());
            log.info("Plugin - [{}/{}] revision {} 로드", app.applicationId, pluginId, revision);
        }
        catch (Exception e) {
            log.error("Plugin - [{}/{}] revision {} 로드 실패", app.applicationId, pluginId, revision, e);
            app.states.put(pluginId, new LocalPluginState(revision, LocalPluginState.FAILED, "로드 실패: " + messageOf(e)));
        }
    }

    private void start(AppPlugins app, PluginWrapper wrapper, long revision) {
        String pluginId = wrapper.getPluginId();
        String message = null;
        try {
            PluginState state = app.manager.startPlugin(pluginId);
            if(state != PluginState.STARTED) {
                Throwable failed = wrapper.getFailedException();
                message = "시작 실패" + (failed == null ? " [" + state + "]" : ": " + messageOf(failed));
            }
            else if(app.manager.getExtensionClasses(WorkflowNodePlugin.class, pluginId).isEmpty()) {
                message = "WorkflowNodePlugin 구현체(@Extension)를 찾을 수 없습니다.";
            }
        }
        catch (Exception e) {
            log.error("Plugin - [{}/{}] 시작 실패", app.applicationId, pluginId, e);
            message = "시작 실패: " + messageOf(e);
        }
        app.states.put(pluginId, stateOf(app, pluginId, revision, message));
    }

    private void stop(AppPlugins app, String pluginId, long revision) {
        String message = null;
        try {
            app.manager.stopPlugin(pluginId);
        }
        catch (Exception e) {
            log.error("Plugin - [{}/{}] 중지 실패", app.applicationId, pluginId, e);
            message = "중지 실패: " + messageOf(e);
        }
        app.states.put(pluginId, stateOf(app, pluginId, revision, message));
    }

    private void unload(AppPlugins app, String pluginId) {
        if(app.manager.getPlugin(pluginId) == null) {
            return;
        }
        try {
            app.manager.stopPlugin(pluginId);
        }
        catch (Exception e) {
            log.warn("Plugin - [{}/{}] 내리기 전 중지 실패", app.applicationId, pluginId, e);
        }
        try {
            app.manager.unloadPlugin(pluginId);
        }
        catch (Exception e) {
            log.warn("Plugin - [{}/{}] 언로드 실패", app.applicationId, pluginId, e);
        }
    }

    private LocalPluginState stateOf(AppPlugins app, String pluginId, long revision, String message) {
        PluginWrapper wrapper = app.manager.getPlugin(pluginId);
        String state = (wrapper == null) ? LocalPluginState.NOT_LOADED : wrapper.getPluginState().name();
        return new LocalPluginState(revision, state, message);
    }

    private AppPlugins app(String applicationId) {
        return apps.computeIfAbsent(applicationId, id -> {
            Path directory = pluginsDirectory(id);
            try {
                Files.createDirectories(directory);
            }
            catch (IOException e) {
                throw new UncheckedIOException("플러그인 경로를 만들 수 없습니다. [" + directory + "]", e);
            }
            return new AppPlugins(id, directory, new HostPluginManager(directory));
        });
    }

    private static void writeJar(Path jar, byte[] content) throws IOException {
        Files.createDirectories(jar.getParent());
        Path temp = jar.resolveSibling(jar.getFileName() + ".tmp");
        Files.write(temp, content);
        Files.move(temp, jar, StandardCopyOption.REPLACE_EXISTING);
    }

    // pluginId 의 jar 중 keepFileName 이 아닌 것을 지운다 (keepFileName 이 null 이면 전부)
    private static void deleteJars(AppPlugins app, String pluginId, String keepFileName) {
        String prefix = pluginId + "-";
        try (DirectoryStream<Path> files = Files.newDirectoryStream(app.directory, "*" + JAR_SUFFIX)) {
            for(Path file : files) {
                String name = file.getFileName().toString();
                if(name.startsWith(prefix) && isRevisionSuffix(name.substring(prefix.length())) && !name.equals(keepFileName)) {
                    deleteQuietly(file);
                }
            }
        }
        catch (IOException e) {
            log.warn("Plugin - [{}] 지난 jar 정리 실패", app.directory, e);
        }
    }

    private static void cleanupDirectory(Path directory, Set<String> keepFileNames) {
        if(!Files.isDirectory(directory)) {
            return;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*" + JAR_SUFFIX)) {
            for(Path file : files) {
                if(!keepFileNames.contains(file.getFileName().toString())) {
                    deleteQuietly(file);
                }
            }
        }
        catch (IOException e) {
            log.warn("Plugin - [{}] 플러그인 경로 정리 실패", directory, e);
        }
    }

    private static boolean isRevisionSuffix(String rest) {
        // "{revision}.jar"
        if(!rest.endsWith(JAR_SUFFIX)) return false;
        String number = rest.substring(0, rest.length() - JAR_SUFFIX.length());
        return !number.isEmpty() && number.chars().allMatch(Character::isDigit);
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        }
        catch (IOException e) {
            // Windows 에서 아직 잠겨 있는 jar. 다음 동기화 때 다시 지운다
            log.debug("Plugin - jar 삭제 보류 [{}]: {}", file, e.getMessage());
        }
    }

    static String jarFileName(String pluginId, long revision) {
        return pluginId + "-" + revision + JAR_SUFFIX;
    }

    private static String messageOf(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = (root.getMessage() == null) ? root.getClass().getSimpleName() : root.getMessage();
        return message.length() > 900 ? message.substring(0, 900) : message;
    }

    private static class AppPlugins {
        private final String applicationId;
        private final Path directory;
        private final HostPluginManager manager;
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        // 로드를 시도한 revision (성공/실패 모두). 같은 revision 을 다시 로드하지 않는 기준
        private final Map<String, Long> attemptedRevisions = new ConcurrentHashMap<>();
        private final Map<String, LocalPluginState> states = new ConcurrentHashMap<>();

        private AppPlugins(String applicationId, Path directory, HostPluginManager manager) {
            this.applicationId = applicationId;
            this.directory = directory;
            this.manager = manager;
        }
    }
}
