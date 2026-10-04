package com.prometis.appbuilder.platform.join;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 가입 인증코드/초대 메일. 플랫폼 공통 메일 서버(application.yml 의 spring.mail.*)로 보낸다.
 * spring.mail.host 가 없으면 JavaMailSender 가 만들어지지 않으므로, 개발 환경에서는 메일 대신 서버 로그에 남긴다.
 */
@Slf4j
@Component
public class JoinMailSender {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final JoinProperties properties;
    private final String mailUsername;

    public JoinMailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            JoinProperties properties,
            @Value("${spring.mail.username:}") String mailUsername
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
        this.mailUsername = mailUsername;
    }

    /**
     * 인증코드 메일. 보내지 못하면 가입을 진행할 수 없으므로 JoinException 을 던진다.
     * @param purpose 메일 제목/본문에 쓸 가입 이름 (예: "App Builder 가입", "인사관리 관리자 합류")
     */
    public void sendVerificationCode(String to, String purpose, String code, Duration ttl) throws JoinException {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if(mailSender == null) {
            log.warn("[가입 인증] spring.mail 설정이 없어 인증 메일을 보내지 않고 로그로 남깁니다. to={}, code={}", to, code);
            return;
        }

        String text = String.join("\n",
                purpose + " 인증코드입니다.",
                "",
                "인증코드: " + code,
                "",
                "가입 화면에 인증코드를 입력해주세요. 인증코드는 " + describe(ttl) + " 동안 유효합니다.",
                "본인이 요청하지 않았다면 이 메일을 무시하세요."
        );

        try {
            mailSender.send(message(to, "[App Builder] " + purpose + " 인증코드", text));
        }
        catch (MailException e) {
            log.error("[가입 인증] 인증 메일 발송 실패. to={}", to, e);
            throw new JoinException("인증 메일을 보내지 못했습니다. 메일 주소를 확인하거나 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 초대 메일. 초대 링크는 화면에서 복사해 직접 전달할 수도 있으므로, 보내지 못해도 초대는 남기고 false 를 돌려준다.
     * @return 메일을 보냈으면 true
     */
    public boolean sendInvitation(String to, String applicationName, String roleLabel, String inviterName, String link, Duration ttl) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if(mailSender == null) {
            log.warn("[초대] spring.mail 설정이 없어 초대 메일을 보내지 않고 로그로 남깁니다. to={}, link={}", to, link);
            return false;
        }

        String inviter = inviterName == null || inviterName.isBlank() ? "애플리케이션 관리자" : inviterName + "님";
        String text = String.join("\n",
                inviter + "이(가) " + applicationName + " 애플리케이션의 " + roleLabel + "로 초대했습니다.",
                "",
                "아래 링크에서 가입하거나 기존 계정으로 로그인해 초대를 수락해주세요.",
                link,
                "",
                "초대 링크는 " + describe(ttl) + " 동안, 이 메일 주소(" + to + ")로 한 번만 쓸 수 있습니다.",
                "초대받을 이유가 없다면 이 메일을 무시하세요."
        );

        try {
            mailSender.send(message(to, "[App Builder] " + applicationName + " " + roleLabel + " 초대", text));
            return true;
        }
        catch (MailException e) {
            log.error("[초대] 초대 메일 발송 실패. to={}", to, e);
            return false;
        }
    }

    private SimpleMailMessage message(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        String from = properties.mailFrom().isEmpty() ? mailUsername : properties.mailFrom();
        if(from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        return message;
    }

    // 5m → "5분", 7d → "7일", 90s → "90초"
    static String describe(Duration ttl) {
        if(ttl.toSeconds() % 86400 == 0) {
            return ttl.toDays() + "일";
        }
        if(ttl.toSeconds() % 3600 == 0) {
            return ttl.toHours() + "시간";
        }
        if(ttl.toSeconds() % 60 == 0) {
            return ttl.toMinutes() + "분";
        }
        return ttl.toSeconds() + "초";
    }
}
