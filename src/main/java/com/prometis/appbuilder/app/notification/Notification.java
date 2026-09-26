package com.prometis.appbuilder.app.notification;

import com.prometis.appbuilder.app.expression.ParameterExpression;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="NOTIFICATION_UQ",columnNames={"notificationName"})})
@Entity
public class Notification {
    private static final String TO_ALIAS_DEFAULT = "to_address";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String notificationName;

    @Column(nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false, length = 100)
    private String dataSourceName;
    @Column
    private Boolean enableSend;

    @Column(length = 1000)
    private String subject;

    @Lob
    @Column
    private String content;

    @Column(length = 100)
    private String toAlias;

    public void update(
            String notificationName,
            String displayName,
            String dataSourceName,
            String toAlias,
            String subject,
            String content,
            Boolean enableSend
    ) {
        this.notificationName = notificationName;
        this.displayName = displayName;
        this.dataSourceName = dataSourceName;
        this.subject = subject;
        this.content = content;
        this.toAlias = toAlias;
        this.enableSend = enableSend;
    }

    public NotificationMessage createMessage(ParameterExpression expression, NotificationRequest params) {
        NotificationMessage.NotificationMessageBuilder builder = NotificationMessage.builder();

        String toAlias = TO_ALIAS_DEFAULT;
        if (this.toAlias != null && !this.toAlias.isEmpty()) {
            toAlias = this.toAlias;
        }

        builder.to((String) params.getParams().get(toAlias));

        // 제목 표현식 적용
        builder.subject(expression.resolve(this.subject, params.getParams()));

        // 내용 표현식 적용
        builder.content(expression.resolve(this.content, params.getParams()));

        return builder.build();
    }
}
