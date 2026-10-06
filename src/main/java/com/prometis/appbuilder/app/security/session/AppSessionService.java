package com.prometis.appbuilder.app.security.session;

import com.prometis.appbuilder.app.security.AppSecurityErrorMessage;
import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import com.prometis.appbuilder.app.security.company.CompanyService;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppSessionService {
    private final AppUserRepository appUserRepository;
    private final AppSessionRepository appSessionRepository;
    private final CompanyService companyService;

    public AppSession registerSession(String applicationId, AuthenticatedUser user) {
        Optional<AppSession> appSessionOptional = appSessionRepository.findByApplicationIdAndUserId(applicationId, user.getUserId());

        AppSession session;
        //세션이 비어있을때만 등록. 추 후 만료일에 따라 갱신하도록.
        if(appSessionOptional.isEmpty()) {
            Optional<AppUser> appUserOptional = appUserRepository.findByApplicationIdAndUserId(applicationId,user.getUserId());
            String companyCode = appUserOptional.map(AppUser::getCompanyCode).orElse(null);

            AppSession saveData = AppSession.builder()
                    .applicationId(applicationId)
                    .userId(user.getUserId())
                    .companyCode(companyCode)
                    .createAt(LocalDateTime.now())
                    .build();

            session = appSessionRepository.save(saveData);
        }
        else {
            session = appSessionOptional.get();
        }

        return session;
    }

    /**
     * 세션에 선택된 회사코드. 세션이 없으면 AppUser 의 회사코드.
     */
    public String getCompanyCode(String applicationId, Long userId) {
        Optional<AppSession> appSessionOptional = appSessionRepository.findByApplicationIdAndUserId(applicationId, userId);
        if(appSessionOptional.isPresent()) {
            return appSessionOptional.get().getCompanyCode();
        }

        return appUserRepository.findByApplicationIdAndUserId(applicationId, userId)
                .map(AppUser::getCompanyCode)
                .orElse(null);
    }

    public AppSession changeCompany(String applicationId, AuthenticatedUser user, String paramsCompanyCode) throws BusinessException {
        Optional<AppSession> appSessionOptional = appSessionRepository.findByApplicationIdAndUserId(applicationId, user.getUserId());
        if(appSessionOptional.isEmpty()) {
            throw new BusinessException(AppSecurityErrorMessage.NOT_FOUND_SESSION);
        }

        AppSession appSession = appSessionOptional.get();

        String companyCode;
        if(paramsCompanyCode == null || paramsCompanyCode.isEmpty()) {
            Optional<AppUser> appUserOptional = appUserRepository.findByApplicationIdAndUserId(applicationId,user.getUserId());
            companyCode = appUserOptional.map(AppUser::getCompanyCode).orElse(null);
        }
        else {
            boolean hasCompany = companyService.hasUserCompany(applicationId, user.getUserId(), paramsCompanyCode);
            if(!hasCompany) {
                throw new BusinessException(AppSecurityErrorMessage.NOT_PERMISSION_COMPANY);
            }

            companyCode = paramsCompanyCode;
        }

        appSession.changeCompany(companyCode);

        return appSessionRepository.save(appSession);
    }
}
