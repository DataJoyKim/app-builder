package com.prometis.appbuilder.security.service;

import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserService;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.domain.RefreshTokenStore;
import com.prometis.appbuilder.security.dto.AuthTokenResponse;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.exception.SecurityErrorMessage;
import com.prometis.appbuilder.security.repository.RefreshTokenStoreRepository;
import com.prometis.appbuilder.security.token.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserService userService;
    private final RefreshTokenStoreRepository refreshTokenStoreRepository;
    private final JwtProvider jwtProvider;

    /**
     * 토큰으로 사용자만 확인한다. 애플리케이션 권한은 부여하지 않는다.
     * 플랫폼 콘솔용
     */
    @Transactional(readOnly = true)
    public AuthenticatedUser authentication(String accessToken) throws SecurityBusinessException {
        if(accessToken == null) {
            throw new SecurityBusinessException(SecurityErrorMessage.NOT_LOGIN);
        }

        // 토큰 유효성검사
        jwtProvider.validateToken(accessToken);

        // 토큰 파싱
        Long userId = jwtProvider.parseAccessToken(accessToken);

        // 사용자 정보 조회
        User user = userService.getUserByUserId(userId);
        if(user == null) {
            throw new SecurityBusinessException(SecurityErrorMessage.NOT_FOUND_USER);
        }

        // 인증된 유저 생성
        return AuthenticatedUser.createAuthenticatedUser(user);
    }

    public AuthTokenResponse refreshToken(String refreshToken) throws SecurityBusinessException {
        jwtProvider.validateToken(refreshToken);

        Long userId = jwtProvider.getUserIdToRefreshToken(refreshToken);

        RefreshTokenStore savedRefreshTokenStore = refreshTokenStoreRepository.findByUserId(userId)
                                                        .orElseThrow();

        if (!refreshToken.equals(savedRefreshTokenStore.getRefreshToken())) {
            throw new SecurityBusinessException(SecurityErrorMessage.DIFF_REFRESH_TOKEN);
        }

        User user = userService.getUserByUserId(userId);
        if(user == null) {
            throw new SecurityBusinessException(SecurityErrorMessage.NOT_FOUND_USER);
        }

        AuthenticatedUser authenticatedUser = AuthenticatedUser.createAuthenticatedUser(user);

        String newAccessToken = jwtProvider.generateAccessToken(authenticatedUser.getUserId());

        return AuthTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
