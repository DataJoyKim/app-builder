package com.prometis.appbuilder.platform.join;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * application.yml 의 platform.join 설정 (공개 가입 /signup, 애플리케이션 관리자 초대 합류 /{applicationId}/console/join).
 *
 * @param verificationCodeTtl 메일로 보낸 인증코드의 유효시간 (예: 5m, 300s)
 * @param maxVerifyAttempts   인증코드를 틀릴 수 있는 횟수. 넘으면 그 요청은 버려지고 인증코드를 다시 받아야 한다
 * @param resendCooldown      같은 이메일로 인증코드를 다시 받기까지 기다려야 하는 시간. 0 이면 제한 없음 (메일 폭탄 방지)
 * @param invitationTtl       초대 링크의 유효기간 (예: 7d)
 * @param mailFrom            메일 보내는 사람. 비어 있으면 spring.mail.username 을 쓴다
 * @param publicBaseUrl       초대 메일에 넣을 링크의 앞부분 (예: https://builder.example.com). 비어 있으면 초대한 요청의 주소를 쓴다
 */
@ConfigurationProperties(prefix = "platform.join")
public record JoinProperties(
        Duration verificationCodeTtl,
        Integer maxVerifyAttempts,
        Duration resendCooldown,
        Duration invitationTtl,
        String mailFrom,
        String publicBaseUrl
) {
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_VERIFY_ATTEMPTS = 5;
    private static final Duration DEFAULT_RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration DEFAULT_INVITATION_TTL = Duration.ofDays(7);

    public JoinProperties {
        if(verificationCodeTtl == null || verificationCodeTtl.isZero() || verificationCodeTtl.isNegative()) {
            verificationCodeTtl = DEFAULT_TTL;
        }
        if(maxVerifyAttempts == null || maxVerifyAttempts < 1) {
            maxVerifyAttempts = DEFAULT_MAX_VERIFY_ATTEMPTS;
        }
        if(resendCooldown == null || resendCooldown.isNegative()) {
            resendCooldown = DEFAULT_RESEND_COOLDOWN;
        }
        if(invitationTtl == null || invitationTtl.isZero() || invitationTtl.isNegative()) {
            invitationTtl = DEFAULT_INVITATION_TTL;
        }
        mailFrom = mailFrom == null ? "" : mailFrom.trim();
        publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
    }
}
