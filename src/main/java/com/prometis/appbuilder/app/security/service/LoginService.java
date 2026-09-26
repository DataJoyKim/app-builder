package com.prometis.appbuilder.app.security.service;

import com.prometis.appbuilder.app.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.app.security.domain.Client;
import com.prometis.appbuilder.app.security.domain.RefreshTokenStore;
import com.prometis.appbuilder.app.security.dto.LoginRequest;
import com.prometis.appbuilder.app.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.app.security.exception.SecurityErrorMessage;
import com.prometis.appbuilder.app.security.repository.RefreshTokenStoreRepository;
import com.prometis.appbuilder.app.security.token.AuthTokenResponse;
import com.prometis.appbuilder.app.security.token.JwtProvider;
import com.prometis.appbuilder.app.user.User;
import com.prometis.appbuilder.app.user.UserService;
import com.prometis.core.crypto.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStoreRepository refreshTokenStoreRepository;

    public AuthTokenResponse login(LoginRequest loginRequest, Client client) throws SecurityBusinessException {

        User user = userService.getUserByLoginId(loginRequest.getLoginId());
        if(user == null) {
            throw new SecurityBusinessException(SecurityErrorMessage.NOT_FOUND_USER);
        }

        if(!passwordEncoder.matches(loginRequest.getPassword(),user.getPassword())) {
            throw new SecurityBusinessException(SecurityErrorMessage.FAULT_PASSWORD);
        }

        AuthenticatedUser authenticatedUser = AuthenticatedUser.createAuthenticatedUser(user);

        String accessToken = jwtProvider.generateAccessToken(authenticatedUser.getUserId());

        String refreshToken = jwtProvider.generateRefreshToken(authenticatedUser.getUserId(), client);

        Optional<RefreshTokenStore> savedRefreshTokenStore = refreshTokenStoreRepository.findByUserId(user.getId());
        RefreshTokenStore store;
        if(savedRefreshTokenStore.isEmpty()) {
            store = RefreshTokenStore.createRefreshTokenStore(user.getId(), refreshToken);
        }
        else {
            store = savedRefreshTokenStore.get();
            store.updateRefreshToken(refreshToken);
        }

        refreshTokenStoreRepository.save(store);

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
