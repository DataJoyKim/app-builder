package com.prometis.appbuilder.restapi;

import com.prometis.appbuilder.restapi.code.ParameterIn;
import com.prometis.appbuilder.restapi.code.SchemaDataType;
import jakarta.persistence.*;
import lombok.*;

/**
 * REST API 가 받는 파라미터 하나와 그 스키마, 그리고 값을 어느 요청메시지에 담을지.
 *
 * 본문(BODY) 필드는 parentKey 로 트리를 이룬다. parentKey 가 비어있으면 본문 최상위의 필드다.
 * 저장 시 전부 지우고 다시 넣기 때문에 DB id 는 매번 바뀐다. 그래서 부모 연결은 id 가 아니라 paramKey 로 한다.
 *
 * 값이 담기는 곳
 * - messageId 가 비어있으면 가장 가까운 상위(본문 최상위는 RestApi.bodyMessageId)의 행에 mappingKey 로 담긴다.
 * - OBJECT/ARRAY 필드에 messageId 를 적으면 그 필드가 별도 요청메시지가 된다(OBJECT 는 한 행, ARRAY 는 원소마다 한 행).
 * - PATH/QUERY/HEADER 는 반드시 messageId 를 적어야 한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class RestApiParameter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "REST_API_ID", nullable = false)
    private Long restApiId;

    // 열거형은 컨버터로 담는다. @Enumerated 는 허용값 CHECK 제약을 만들어 값을 추가할 때 기존 DB 저장이 깨진다(EnumStringConverter 참고).
    @Convert(converter = ParameterIn.Converter.class)
    @Column(nullable = false, length = 20)
    private ParameterIn paramIn;

    @Column(nullable = false, length = 100)
    private String paramKey;

    @Column(length = 100)
    private String parentKey;

    @Column(nullable = false, length = 200)
    private String name;

    @Convert(converter = SchemaDataType.Converter.class)
    @Column(nullable = false, length = 20)
    private SchemaDataType dataType;

    @Column(nullable = false)
    private Boolean required;

    @Column(length = 1000)
    private String defaultValue;

    @Column(length = 100)
    private String messageId;

    // 요청메시지에 담길 때의 키. 비어있으면 name 을 그대로 쓴다. (헤더 X-User-Id -> user_id 처럼 바꿔 담을 때 쓴다)
    @Column(length = 200)
    private String mappingKey;

    @Column(length = 1000)
    private String description;

    @Column
    private Integer orderNum;

    public boolean isRequired() {
        return Boolean.TRUE.equals(required);
    }

    public String resolveMappingKey() {
        return (mappingKey == null || mappingKey.isBlank()) ? name : mappingKey;
    }

    public boolean hasMessageId() {
        return messageId != null && !messageId.isBlank();
    }

    public boolean isRootBodyField() {
        return ParameterIn.BODY.equals(paramIn) && (parentKey == null || parentKey.isBlank());
    }
}
