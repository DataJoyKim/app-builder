package com.prometis.appbuilder.app.plugin.runtime;

import com.prometis.appbuilder.app.plugin.PluginException;
import com.prometis.appbuilder.app.plugin.PluginJarInfo;
import com.prometis.appbuilder.app.plugin.PluginJarInspector;
import com.prometis.appbuilder.app.plugin.runtime.PluginRuntime.DesiredPlugin;
import com.prometis.plugin.api.PluginContext;
import com.prometis.plugin.api.PluginRequest;
import com.prometis.plugin.api.PluginResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * plugin-template 이 만든 실제 jar 로 로드 / 실행 / 핫리로드 / 중지 / 삭제를 확인한다.
 */
class PluginRuntimeTest {
    private static final String APP = "app1";
    private static final String PLUGIN_ID = "template-plugin";

    @TempDir
    Path root;

    private PluginRuntime runtime;
    private byte[] templateJar;

    @BeforeEach
    void setUp() throws IOException {
        String jarPath = System.getProperty("plugin.template.jar");
        assertThat(jarPath).as("gradle test 가 plugin.template.jar 를 넘겨줘야 한다").isNotBlank();
        templateJar = Files.readAllBytes(Paths.get(jarPath));
        runtime = new PluginRuntime(root.toString());
    }

    @AfterEach
    void tearDown() {
        runtime.shutdown();
    }

    @Test
    void inspectorReadsManifestOfTemplateJar() {
        PluginJarInfo info = PluginJarInspector.inspect(templateJar);

        assertThat(info.pluginId()).isEqualTo(PLUGIN_ID);
        assertThat(info.version()).isEqualTo("1.0.0");
    }

    @Test
    void inspectorRejectsNonJar() {
        assertThatThrownBy(() -> PluginJarInspector.inspect("not a jar".getBytes()))
                .isInstanceOf(PluginException.class);
    }

    @Test
    void loadStartExecuteReloadStopAndRemove() {
        AtomicInteger jarReads = new AtomicInteger();

        LocalPluginState started = runtime.apply(new DesiredPlugin(APP, PLUGIN_ID, 1, true), () -> {
            jarReads.incrementAndGet();
            return templateJar;
        });
        assertThat(started.state()).isEqualTo("STARTED");
        assertThat(started.revision()).isEqualTo(1L);
        assertThat(started.message()).isNull();
        assertThat(root.resolve(APP).resolve("plugins").resolve("template-plugin-1.jar")).exists();

        PluginResult echo = execute("template/echo", List.of(Map.of("name", "kim")));
        assertThat(echo.isSuccess()).isTrue();
        assertThat(echo.getContents()).hasSize(1);
        assertThat(echo.getContents().get(0)).containsEntry("name", "kim").containsEntry("applicationId", APP).containsEntry("userId", 7L);

        PluginResult unknown = execute("template/none", List.of());
        assertThat(unknown.isSuccess()).isFalse();

        // 같은 revision 이면 jar 를 다시 읽지 않는다
        runtime.apply(new DesiredPlugin(APP, PLUGIN_ID, 1, true), () -> {
            jarReads.incrementAndGet();
            return templateJar;
        });
        assertThat(jarReads).hasValue(1);

        // 핫리로드: revision 이 오르면 새 jar 로 다시 로드한다
        LocalPluginState reloaded = runtime.apply(new DesiredPlugin(APP, PLUGIN_ID, 2, true), () -> {
            jarReads.incrementAndGet();
            return templateJar;
        });
        assertThat(jarReads).hasValue(2);
        assertThat(reloaded.state()).isEqualTo("STARTED");
        assertThat(reloaded.revision()).isEqualTo(2L);
        assertThat(execute("template/echo", List.of(Map.of("a", 1))).isSuccess()).isTrue();

        // 데이터소스가 없으면 플러그인이 실패 결과를 돌려준다
        PluginResult ping = execute("template/db-ping", List.of(Map.of("dataSourceName", "nothing")));
        assertThat(ping.isSuccess()).isFalse();
        assertThat(ping.getMessage()).contains("nothing");

        LocalPluginState stopped = runtime.apply(new DesiredPlugin(APP, PLUGIN_ID, 2, false), () -> templateJar);
        assertThat(stopped.state()).isEqualTo("STOPPED");
        assertThatThrownBy(() -> execute("template/echo", List.of()))
                .isInstanceOf(PluginUnavailableException.class);

        runtime.remove(APP, PLUGIN_ID);
        assertThat(runtime.state(APP, PLUGIN_ID).state()).isEqualTo(LocalPluginState.NOT_LOADED);
    }

    @Test
    void brokenJarIsReportedAsFailedAndNotRetriedForSameRevision() {
        AtomicInteger reads = new AtomicInteger();

        LocalPluginState failed = runtime.apply(new DesiredPlugin(APP, "broken", 1, true), () -> {
            reads.incrementAndGet();
            return "broken".getBytes();
        });
        assertThat(failed.state()).isEqualTo(LocalPluginState.FAILED);
        assertThat(failed.message()).isNotBlank();

        runtime.apply(new DesiredPlugin(APP, "broken", 1, true), () -> {
            reads.incrementAndGet();
            return "broken".getBytes();
        });
        assertThat(reads).hasValue(1);
    }

    @Test
    void rejectsApplicationIdThatEscapesRoot() {
        assertThatThrownBy(() -> runtime.pluginsDirectory("../other")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> runtime.pluginsDirectory("a/b")).isInstanceOf(IllegalArgumentException.class);
    }

    private PluginResult execute(String pluginPath, List<Map<String, Object>> contents) {
        PluginRequest request = new PluginRequest(contents.stream().map(row -> (Map<String, Object>) new java.util.HashMap<>(row)).toList(),
                Map.of(PluginRequest.ATTR_USER_ID, 7L));
        PluginContext context = new PluginContext() {
            @Override
            public String getApplicationId() {
                return APP;
            }

            @Override
            public Optional<DataSource> findDataSource(String dataSourceName) {
                return Optional.empty();
            }
        };
        return runtime.execute(APP, PLUGIN_ID, plugin -> plugin.execute(pluginPath, request, context));
    }
}
