package com.prometis.appbuilder.app.view.dto;

import com.prometis.appbuilder.app.security.domain.AuthenticatedUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class ProfileDto {
    @Getter @AllArgsConstructor @Builder
    public static class ProfileResponse {
        private String userName;

        public static ProfileResponse of(AuthenticatedUser user) {
            return ProfileResponse.builder()
                    .userName(user.getUserName())
                    .build();
        }
    }
}
