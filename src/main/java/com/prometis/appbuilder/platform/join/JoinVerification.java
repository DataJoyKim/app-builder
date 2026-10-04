package com.prometis.appbuilder.platform.join;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 메일 인증을 기다리는 가입 요청.
 * - 공개 가입(/signup) : applicationId, invitationId 가 없다. 인증되면 계정만 만든다
 * - 초대 합류 : 초대한 애플리케이션과 초대(AppInvitation)를 가리킨다. 인증되면 계정을 만들고 초대받은 권한으로 등록한다
 * - 앱 가입 (/{applicationId}/signup) : 애플리케이션만 가리킨다. 인증되면 계정을 만들고 그 앱의 가입 방식대로 가입/신청한다
 * 인증코드를 맞히면 이 내용으로 User 를 만들고 지운다. 인증코드와 비밀번호는 해시로만 가지고 있는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="JOIN_VERIFICATION_UQ",columnNames={"verificationKey"})})
@Entity
public class JoinVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 화면이 인증 단계에서 이 요청을 가리키는 값 (추측할 수 없는 UUID)
    @Column(nullable = false, length = 100)
    private String verificationKey;

    // 가입 종류 (TYPE_*). 이 값이 생기기 전 행은 비어 있어서 getJoinType() 이 다른 칸으로 가린다
    @Column(length = 30)
    private String joinType;

    // 초대 합류, 앱 가입일 때만 있다
    @Column(length = 100)
    private String applicationId;

    // 초대 합류일 때만 있다 (AppInvitation.id)
    @Column
    private Long invitationId;

    @Column(nullable = false, length = 100)
    private String loginId;

    @Column(nullable = false, length = 100)
    private String userName;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String encodedPassword;

    @Column(nullable = false, length = 100)
    private String encodedCode;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Integer failCount;

    @Column
    private LocalDateTime createdAt;

    public static final String TYPE_SIGNUP = "SIGNUP";
    public static final String TYPE_INVITATION = "INVITATION";
    public static final String TYPE_APP_SIGNUP = "APP_SIGNUP";

    public String getJoinType() {
        if(joinType != null && !joinType.isBlank()) {
            return joinType;
        }
        if(invitationId != null) {
            return TYPE_INVITATION;
        }
        return applicationId == null ? TYPE_SIGNUP : TYPE_APP_SIGNUP;
    }

    public boolean isInvitation() {
        return TYPE_INVITATION.equals(getJoinType());
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public void increaseFailCount() {
        this.failCount = (failCount == null ? 0 : failCount) + 1;
    }
}
