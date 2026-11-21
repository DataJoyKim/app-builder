package com.prometis.appbuilder.app.plugin;

import com.prometis.appbuilder.app.plugin.runtime.PluginRuntime;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PluginProperties.class)
public class PluginConfig {

    // 종료 시 플러그인을 모두 내려 jar 잠금을 푼다 (devtools 재시작 포함)
    @Bean(destroyMethod = "shutdown")
    public PluginRuntime pluginRuntime(PluginProperties properties) {
        return new PluginRuntime(properties.rootPath());
    }
}
