package com.prometis.appbuilder;

import com.prometis.appbuilder.console.app.AppConsoleAccessValidator;
import com.prometis.appbuilder.console.app.AppConsoleSecurityFilter;
import com.prometis.appbuilder.console.platform.PlatformConsoleAccessValidator;
import com.prometis.appbuilder.console.platform.PlatformConsoleSecurityFilter;
import com.prometis.appbuilder.platform.application.ApplicationGuard;
import com.prometis.appbuilder.security.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {
    @Autowired
    AuthenticationService authenticationService;
    @Autowired
    AppConsoleAccessValidator appConsoleAccessValidator;
    @Autowired
    ApplicationGuard applicationGuard;
    @Autowired
    PlatformConsoleAccessValidator platformConsoleAccessValidator;

    @Bean
    public FilterRegistrationBean<AppConsoleSecurityFilter> consoleSecurityFilterRegistration() {
        FilterRegistrationBean<AppConsoleSecurityFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AppConsoleSecurityFilter(authenticationService, appConsoleAccessValidator, applicationGuard));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);

        return registration;
    }

    @Bean
    public FilterRegistrationBean<PlatformConsoleSecurityFilter> platformConsoleSecurityFilterRegistration() {
        FilterRegistrationBean<PlatformConsoleSecurityFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new PlatformConsoleSecurityFilter(authenticationService, platformConsoleAccessValidator));
        registration.addUrlPatterns("/console/*");
        registration.setOrder(2);

        return registration;
    }
}
