package com.prometis.appbuilder.platform.join;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 애플리케이션 초대. 애플리케이션 관리자가 콘솔에서 이메일과 권한(관리자/사용자)을 지정해 만들고, 링크(/{applicationId}/console/join?token=...)를 전달한다.
 * 초대받은 이메일로만 합류할 수 있고, 한 번 쓰면(acceptedAt) 다시 쓸 수 없다. 취소하면 행을 지운다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="APP_INVITATION_UQ",columnNames={"token"})})
@Entity
public class AppInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 링크에 들어가는 추측할 수 없는 값
    @Column(nullable = false, length = 100)
    private String token;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column(nullable = false, length = 100)
    private String email;

    // 합류하면 받는 권한 (AppUser.AUTHORITY_*). 권한 선택이 생기기 전에 만든 초대는 비어 있고 관리자 초대로 본다
    @Column(length = 100)
    private String authority;

    // 합류하면 소속될 회사 (Company.companyCode). 회사 없이 만든 초대는 비어 있다
    @Column(length = 100)
    private String companyCode;

    @Column
    private Long invitedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime acceptedAt;

    @Column
    private Long acceptedUserId;

    public String getAuthority() {
        return authority == null || authority.isBlank() ? AppUser.AUTHORITY_APPLICATION_ADMIN : authority;
    }

    public boolean isAdminInvitation() {
        return AppUser.AUTHORITY_APPLICATION_ADMIN.equals(getAuthority());
    }

    public boolean isAccepted() {
        return acceptedAt != null;
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isEmailOf(String otherEmail) {
        return otherEmail != null && email.equalsIgnoreCase(otherEmail.trim());
    }

    public void accept(Long userId, LocalDateTime now) {
        this.acceptedAt = now;
        this.acceptedUserId = userId;
    }
}
