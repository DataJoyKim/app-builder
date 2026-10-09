package com.prometis.appbuilder.app.view.dto;

import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class ProfileDto {
    @Getter @AllArgsConstructor @Builder
    public static class ProfileResponse {
        private String userName;
        // 프로필 이미지 링크. 설정이 없거나 사용자의 이미지가 없으면 null
        private String profileImg;

        public static ProfileResponse of(AuthenticatedUser user, String profileImg) {
            return ProfileResponse.builder()
                    .userName(user.getUserName())
                    .profileImg(profileImg)
                    .build();
        }
    }
}
