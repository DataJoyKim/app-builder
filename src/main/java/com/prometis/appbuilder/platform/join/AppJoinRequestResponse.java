package com.prometis.appbuilder.platform.join;

import com.prometis.appbuilder.platform.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 콘솔에 보여줄 가입 신청. 사용자가 지워졌으면 사용자 정보는 비어 있다.
 */
@Getter
@Builder
public class AppJoinRequestResponse {
    private Long id;
    private Long userId;
    private String loginId;
    private String userName;
    private String email;
    private LocalDateTime requestedAt;

    public static AppJoinRequestResponse of(AppJoinRequest request, User user) {
        return AppJoinRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .loginId(user == null ? null : user.getLoginId())
                .userName(user == null ? null : user.getUserName())
                .email(user == null ? null : user.getEmail())
                .requestedAt(request.getRequestedAt())
                .build();
    }
}
