package com.prometis.appbuilder.platform.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApplicationGuard {
    private final ApplicationRepository applicationRepository;

    /**
     * 애플리케이션이 유효한지 검증
     * @param applicationId 애플리케이션ID
     * @throws ApplicationNotFoundException
     */
    public void check(String applicationId) throws ApplicationNotFoundException {
        Optional<Application> applicationOptional = applicationRepository.findByApplicationId(applicationId);
        if(applicationOptional.isEmpty()) {
            throw new ApplicationNotFoundException();
        }
    }
}
