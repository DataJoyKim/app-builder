package com.prometis.appbuilder.app.plugin.runtime;

import org.pf4j.ClassLoadingStrategy;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginLoader;
import org.pf4j.PluginManager;
import org.pf4j.RuntimeMode;
import org.pf4j.util.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 애플리케이션 하나의 pf4j 플러그인 매니저. 플러그인 경로({root}/{applicationId}/plugins)를 통째로 읽지 않고,
 * {@link PluginRuntime} 이 DB 기준으로 jar 파일을 하나씩 load/unload 한다.
 */
class HostPluginManager extends DefaultPluginManager {

    HostPluginManager(Path pluginsRoot) {
        super(pluginsRoot);
    }

    // 개발 모드(pf4j.mode=development)면 pf4j 가 jar 대신 빌드 폴더를 찾으므로 항상 배포 모드로 쓴다
    @Override
    public RuntimeMode getRuntimeMode() {
        return RuntimeMode.DEPLOYMENT;
    }

    // 업로드한 jar 만 다룬다
    @Override
    protected PluginLoader createPluginLoader() {
        return new HostJarPluginLoader(this);
    }

    /**
     * 플러그인 클래스로더의 부모를 호스트 애플리케이션 클래스로더로 잡는다.
     *
     * <p>pf4j 기본 로더는 pf4j 를 읽은 클래스로더를 부모로 쓴다. spring-boot-devtools 가 켜져 있으면 pf4j(jar)는 base 클래스로더,
     * 호스트 코드는 restart 클래스로더에 올라가 서로 다른 WorkflowNodePlugin 을 보게 되므로, 호스트 코드를 읽은 클래스로더를 부모로 둔다.
     * 플러그인 → 의존 플러그인 → 호스트 순(PDA)으로 찾으므로 플러그인이 넣어 온 라이브러리가 호스트 것보다 먼저 쓰인다.</p>
     */
    static class HostJarPluginLoader implements PluginLoader {
        private final PluginManager pluginManager;

        HostJarPluginLoader(PluginManager pluginManager) {
            this.pluginManager = pluginManager;
        }

        @Override
        public boolean isApplicable(Path pluginPath) {
            return Files.exists(pluginPath) && FileUtils.isJarFile(pluginPath);
        }

        @Override
        public ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor) {
            PluginClassLoader classLoader = new PluginClassLoader(
                    pluginManager, pluginDescriptor, HostPluginManager.class.getClassLoader(), ClassLoadingStrategy.PDA);
            classLoader.addFile(pluginPath.toFile());
            return classLoader;
        }
    }
}
