package com.prometis.appbuilder.app.executor.notification;

import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Properties;

@Slf4j
@AllArgsConstructor
@Builder
public class SmtpProvider implements NotificationSender {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private Integer connectionTimeout; //Socket 연결 타임아웃
    private Integer writeTimeout; // Socket Write 타임아웃 (메일본문, 첨부파일 보낼때 대기하는시간)
    private Integer timeout; // Socket Read 타임아웃 (인증완료, 수신완료 상태확인하는용)
    private String sslProtocols;
    private SmtpContentType smtpContentType;
    private SmtpSecurityType smtpSecurityType;

    private JavaMailSenderImpl createMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(this.host);
        mailSender.setPort(this.port);
        mailSender.setUsername(this.username);
        mailSender.setPassword(this.password);

        Properties props = mailSender.getJavaMailProperties();

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", String.valueOf(this.connectionTimeout));
        props.put("mail.smtp.timeout", String.valueOf(this.timeout));
        props.put("mail.smtp.writetimeout", String.valueOf(this.writeTimeout));

        if(this.sslProtocols != null) {
            props.put("mail.smtp.ssl.protocols", this.sslProtocols);
        }

        switch (this.smtpSecurityType) {
            case SSL_TLS:
                props.put("mail.smtp.ssl.enable", "true");
                break;

            case STARTTLS:
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
                break;
        }

        return mailSender;
    }

    @Override
    public SendResult send(String sendId, String to, String subject, String content) {
        if(to == null || to.isEmpty()) {
            return SendResult.builder()
                    .resultType(SendResultType.FAILURE)
                    .errorMessage("메시지 수신자가 지정되지않았습니다.")
                    .build();
        }

        NotificationSenderPrint.printMail(sendId, this.username, to, subject, content);

        try {
            JavaMailSenderImpl mailSender = createMailSender();

            if (this.smtpContentType == null || this.smtpContentType == SmtpContentType.HTML) {

                MimeMessage message = mailSender.createMimeMessage();

                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(this.username);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(content, true);

                mailSender.send(message);
            }
            else if (this.smtpContentType == SmtpContentType.TEXT) {

                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(this.username);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(content);

                mailSender.send(message);
            }

            return SendResult.builder()
                    .resultType(SendResultType.SUCCESS)
                    .build();
        }
        catch (Exception e) {
            log.error("error",e);
            return SendResult.builder()
                    .resultType(SendResultType.FAILURE)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public SendResult validate() {
        JavaMailSenderImpl mailSender = createMailSender();
        try {
            mailSender.testConnection(); // 연결 및 인증 테스트
            return SendResult.builder()
                    .resultType(SendResultType.SUCCESS)
                    .build();
        } catch (Exception e) {
            log.error("error",e);
            return SendResult.builder()
                    .resultType(SendResultType.FAILURE)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
