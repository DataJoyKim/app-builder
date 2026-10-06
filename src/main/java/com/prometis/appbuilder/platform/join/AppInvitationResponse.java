package com.prometis.appbuilder.platform.join;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 콘솔에 보여줄 초대. authority 는 합류하면 받는 권한(APPLICATION_ADMIN/APPLICATION_USER)이다. link 는 아직 쓸 수 있는(PENDING) 초대에만 있다. mailSent 는 초대를 만든 응답에만 있다.
 */
@Getter
@Builder
public class AppInvitationResponse {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private Long id;
    private String email;
    private String authority;
    private String companyCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;
    private String link;
    private Boolean mailSent;

    public static AppInvitationResponse of(AppInvitation invitation, String link, LocalDateTime now, Boolean mailSent) {
        String status = invitation.isAccepted() ? STATUS_ACCEPTED
                : invitation.isExpired(now) ? STATUS_EXPIRED
                : STATUS_PENDING;

        return AppInvitationResponse.builder()
                .id(invitation.getId())
                .email(invitation.getEmail())
                .authority(invitation.getAuthority())
                .companyCode(invitation.getCompanyCode())
                .status(status)
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .acceptedAt(invitation.getAcceptedAt())
                .link(STATUS_PENDING.equals(status) ? link : null)
                .mailSent(mailSent)
                .build();
    }
}
