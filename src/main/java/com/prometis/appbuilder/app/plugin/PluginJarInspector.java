package com.prometis.appbuilder.app.plugin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

/**
 * 업로드한 jar 가 이 호스트에서 쓸 수 있는 pf4j 플러그인인지 저장 전에 확인한다.
 * 실제로 로드해 보지 않고도 흔한 실수(MANIFEST 누락, @Extension 누락, plugin-api 를 jar 에 넣음)를 콘솔에서 바로 알려주려는 것이다.
 */
public final class PluginJarInspector {
    // 파일명({pluginId}-{revision}.jar)에도 쓰이므로 경로 문자를 막는다
    public static final Pattern PLUGIN_ID_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$");

    private static final String EXTENSIONS_INDEX = "META-INF/extensions.idx";
    // 호스트가 제공하는 패키지. 플러그인 jar 에 함께 들어 있으면 플러그인 클래스로더가 자기 것을 먼저 읽어 호스트의 인터페이스와 다른 클래스가 된다
    private static final String[] HOST_PROVIDED_PACKAGES = {"com/prometis/plugin/api/", "org/pf4j/"};

    private PluginJarInspector() {
    }

    public static PluginJarInfo inspect(byte[] jar) {
        Manifest manifest;
        boolean hasExtensionIndex = false;

        try (JarInputStream in = new JarInputStream(new ByteArrayInputStream(jar))) {
            manifest = in.getManifest();

            JarEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                String name = entry.getName();
                if(EXTENSIONS_INDEX.equals(name)) {
                    hasExtensionIndex = true;
                }
                for(String hostPackage : HOST_PROVIDED_PACKAGES) {
                    if(name.startsWith(hostPackage) && name.endsWith(".class")) {
                        throw new PluginException("플러그인 jar 에 호스트가 제공하는 클래스(" + hostPackage.replace('/', '.')
                                + ")가 들어 있습니다. plugin-api 와 pf4j 는 compileOnly 로 빼고 다시 빌드해주세요.");
                    }
                }
            }
        }
        catch (IOException e) {
            throw new PluginException("jar 파일을 읽을 수 없습니다. 올바른 jar 인지 확인해주세요.", e);
        }

        if(manifest == null) {
            throw new PluginException("jar 에 MANIFEST.MF 가 없습니다. Plugin-Id, Plugin-Version 을 MANIFEST 에 넣어주세요.");
        }

        Attributes attributes = manifest.getMainAttributes();
        String pluginId = trim(attributes.getValue("Plugin-Id"));
        String version = trim(attributes.getValue("Plugin-Version"));

        if(pluginId == null) {
            throw new PluginException("MANIFEST 에 Plugin-Id 가 없습니다.");
        }
        if(!PLUGIN_ID_PATTERN.matcher(pluginId).matches()) {
            throw new PluginException("Plugin-Id 는 영문/숫자로 시작하고 영문/숫자/ . _ - 만 쓸 수 있습니다(100자 이내). [" + pluginId + "]");
        }
        if(version == null) {
            throw new PluginException("MANIFEST 에 Plugin-Version 이 없습니다.");
        }
        if(version.length() > 50) {
            throw new PluginException("Plugin-Version 은 50자 이내여야 합니다.");
        }
        if(!hasExtensionIndex) {
            throw new PluginException("jar 에 " + EXTENSIONS_INDEX + " 가 없습니다. WorkflowNodePlugin 구현체에 @Extension 을 붙이고 "
                    + "pf4j 를 annotationProcessor 로 넣어 빌드해주세요.");
        }

        return new PluginJarInfo(
                pluginId,
                version,
                limit(trim(attributes.getValue("Plugin-Provider")), 100),
                limit(trim(attributes.getValue("Plugin-Description")), 500)
        );
    }

    private static String trim(String value) {
        if(value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String limit(String value, int max) {
        return (value == null || value.length() <= max) ? value : value.substring(0, max);
    }
}
