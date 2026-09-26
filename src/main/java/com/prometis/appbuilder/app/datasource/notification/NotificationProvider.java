package com.prometis.appbuilder.app.datasource.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prometis.appbuilder.app.executor.notification.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Table(uniqueConstraints = {@UniqueConstraint(name="DATA_SOURCE_REST_SERVER_UQ",columnNames={"DATA_SOURCE_NAME"})})
@Entity
public class NotificationProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String dataSourceName;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(length = 500)
    private String note;

    @Column(length = 100)
    private NotificationType type;

    @Lob
    @Column
    private String options;

    public NotificationSender createDataSource() throws NotificationCreationException {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Map<String, String> optionsMap = objectMapper.readValue(
                    options,
                    new TypeReference<>() {}
            );

            if(this.type == NotificationType.SMTP) {
                return SmtpProvider.builder()
                        .host(optionsMap.get("host"))
                        .port(Integer.valueOf(optionsMap.get("port")))
                        .username(optionsMap.get("username"))
                        .password(optionsMap.get("password"))
                        .connectionTimeout(Integer.valueOf(optionsMap.get("connectionTimeout")))
                        .writeTimeout(Integer.valueOf(optionsMap.get("writeTimeout")))
                        .timeout(Integer.valueOf(optionsMap.get("timeout")))
                        .smtpContentType(SmtpContentType.valueOf(optionsMap.get("smtpContentType")))
                        .smtpSecurityType(SmtpSecurityType.valueOf(optionsMap.get("smtpSecurityType")))
                        .sslProtocols(optionsMap.get("sslProtocols"))
                        .build();
            }
            else {
                throw new RuntimeException("지원하지않는 타입입니다. ["+this.type+"]");
            }
        }
        catch (Exception e) {
            throw new NotificationCreationException(e);
        }
    }

    public void update(
            String dataSourceName,
            String displayName,
            String note,
            NotificationType type,
            String options
    ) {
        this.dataSourceName= dataSourceName;
        this.displayName = displayName;
        this.note = note;
        this.type = type;
        this.options = options;
    }
}
