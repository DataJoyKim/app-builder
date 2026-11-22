package com.prometis.appbuilder.app.view.log;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 화면(ViewObject) 접근 로그. GET /{applicationId}/pages/{objectCode} 한 번에 한 줄이다.
 * 메뉴는 앱 화면의 메뉴 링크가 붙여 보내는 menuCd 로 알며, 이 애플리케이션에 있는 메뉴일 때만 남긴다(직접 URL, 홈 화면 등은 비어 있다).
 * 화면/메뉴 이름은 나중에 바뀌거나 지워져도 로그를 읽을 수 있도록 접근 당시 값을 복사해 둔다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(indexes = {
        @Index(name = "VIEW_ACCESS_LOG_IX1", columnList = "applicationId,accessedDate"),
        @Index(name = "VIEW_ACCESS_LOG_IX2", columnList = "applicationId,objectCode")
})
@Entity
public class ViewAccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String applicationId;
    @Column(nullable = false, length = 100)
    private String objectCode;
    @Column(length = 200)
    private String objectName;
    @Column(length = 20)
    private String objectType;
    @Column(length = 100)
    private String menuCd;
    @Column(length = 200)
    private String menuNm;
    // 익명 접근이면 비어 있다
    @Column
    private Long userId;
    @Column(length = 200)
    private String userName;
    @Column(length = 64)
    private String clientIp;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ViewAccessResult accessResult;
    @Column(nullable = false)
    private LocalDateTime accessedAt;
    @Column(nullable = false)
    private LocalDate accessedDate;
}
