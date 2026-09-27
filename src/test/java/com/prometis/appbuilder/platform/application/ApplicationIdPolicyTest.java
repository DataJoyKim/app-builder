package com.prometis.appbuilder.platform.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationIdPolicyTest {
    private final ApplicationIdPolicy policy = new ApplicationIdPolicy(
            new ApplicationIdProperties(List.of("console", "console-plat", "pages", "rest", "workflow", "api", "h2-console", "plugins", "ui"))
    );

    @ParameterizedTest
    @ValueSource(strings = {"ehr", "hr-portal", "erp_v2", "app1", "console2", "my-console"})
    void validIds(String applicationId) {
        assertThat(policy.validate(applicationId)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"EHR", "Ehr", "eHr"})
    void rejectsUppercase(String applicationId) {
        assertThat(policy.validate(applicationId)).contains("소문자");
    }

    @ParameterizedTest
    @ValueSource(strings = {"e hr", "ehr/app", "ehr.app", "한글", "ehr!"})
    void rejectsInvalidCharacters(String applicationId) {
        assertThat(policy.validate(applicationId)).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"console", "console-plat", "pages", "rest", "workflow", "api", "h2-console", "plugins", "ui"})
    void rejectsReservedPaths(String applicationId) {
        assertThat(policy.validate(applicationId)).contains("시스템에서 사용하는 경로");
    }

    @Test
    void rejectsBlank() {
        assertThat(policy.validate(null)).isNotNull();
        assertThat(policy.validate("")).isNotNull();
        assertThat(policy.validate("  ")).isNotNull();
    }

    @Test
    void normalizesConfiguredReservedIds() {
        ApplicationIdPolicy configured = new ApplicationIdPolicy(
                new ApplicationIdProperties(Arrays.asList(" /Console ", "PAGES", "", null))
        );

        assertThat(configured.getReservedIds()).containsExactly("console", "pages");
        assertThat(configured.validate("console")).isNotNull();
    }

    @Test
    void noReservedIdsWhenNotConfigured() {
        ApplicationIdPolicy empty = new ApplicationIdPolicy(new ApplicationIdProperties(null));

        assertThat(empty.getReservedIds()).isEmpty();
        assertThat(empty.validate("console")).isNull();
    }

    @Test
    void bindsReservedIdsFromApplicationYml() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))
                .forEach(environment.getPropertySources()::addLast);

        ApplicationIdProperties properties = new Binder(ConfigurationPropertySources.get(environment))
                .bind("platform.application", ApplicationIdProperties.class)
                .get();

        assertThat(properties.reservedIds())
                .contains("console", "console-plat", "pages", "rest", "workflow", "h2-console");
    }
}
