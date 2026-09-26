package com.prometis.appbuilder.app.restapi;

import com.prometis.appbuilder.app.restapi.code.FileContentEncoding;
import com.prometis.appbuilder.app.restapi.code.HttpMethodType;
import com.prometis.appbuilder.app.restapi.code.ResponseType;
import com.prometis.appbuilder.app.restapi.code.SchemaDataType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 워크플로우 하나를 감싸서 외부에 공개하는 REST API 정의.
 * URL_PREFIX{path} 로 들어온 요청을 파라미터 정의(RestApiParameter)대로 검증하고 요청메시지에 담아 workflowCode 워크플로우를 실행한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="REST_API_UQ", columnNames={"API_CODE"})})
@Entity
public class RestApi {
    public static final String URL_PREFIX = "/rest";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "API_CODE", nullable = false, length = 100)
    private String apiCode;

    @Column(nullable = false, length = 200)
    private String displayName;

    // 열거형은 컨버터로 담는다. @Enumerated 는 허용값 CHECK 제약을 만들어 값을 추가할 때 기존 DB 저장이 깨진다(EnumStringConverter 참고).
    @Convert(converter = HttpMethodType.Converter.class)
    @Column(nullable = false, length = 20)
    private HttpMethodType httpMethod;

    // URL_PREFIX 뒤에 붙는 경로. 항상 / 로 시작하고, {name} 으로 경로변수를 받는다. 예) /goals/{goalId}
    @Column(nullable = false, length = 500)
    private String path;

    @Column(nullable = false, length = 100)
    private String workflowCode;

    // 요청 본문의 최상위 모양. null 이면 본문을 받지 않는다.
    @Convert(converter = SchemaDataType.Converter.class)
    @Column(length = 20)
    private SchemaDataType bodyDataType;

    // 본문 최상위가 담길 요청메시지ID. OBJECT 면 한 행, ARRAY 면 원소마다 한 행이 된다.
    @Column(length = 100)
    private String bodyMessageId;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Boolean enabled;

    // 응답유형. null 은 이 설정이 생기기 전에 저장된 API 라 JSON 으로 본다.
    @Convert(converter = ResponseType.Converter.class)
    @Column(length = 20)
    private ResponseType responseType;

    // ---- JSON 응답 설정 ----
    // 응답할 워크플로우 응답메시지ID. 지정하면 contents 에 이 메시지의 내용만(메시지ID 키 없이) 담는다. 비어있으면 contents 전체를 그대로 담는다.
    @Column(length = 100)
    private String responseMessageId;

    // 응답 형태. ARRAY 는 행 목록 전체, OBJECT 는 첫 행 하나(없으면 null). null 은 ARRAY 로 본다.
    @Convert(converter = SchemaDataType.Converter.class)
    @Column(length = 20)
    private SchemaDataType responseDataType;

    // ---- 파일 다운로드(FILE) 설정. 파일 메시지의 첫 행에서 아래 컬럼을 읽는다. ----
    @Column(length = 100)
    private String fileMessageId;

    @Column(length = 200)
    private String fileContentKey;

    @Convert(converter = FileContentEncoding.Converter.class)
    @Column(length = 20)
    private FileContentEncoding fileContentEncoding;

    // 파일명이 담긴 컬럼. 비어있거나 값이 없으면 fileName 을 쓴다.
    @Column(length = 200)
    private String fileNameKey;

    @Column(length = 300)
    private String fileName;

    // Content-Type 이 담긴 컬럼. 비어있거나 값이 없으면 파일명 확장자로 정한다.
    @Column(length = 200)
    private String fileContentTypeKey;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public ResponseType resolveResponseType() {
        return responseType == null ? ResponseType.JSON : responseType;
    }

    public FileContentEncoding resolveFileContentEncoding() {
        return fileContentEncoding == null ? FileContentEncoding.BASE64 : fileContentEncoding;
    }

    public SchemaDataType resolveResponseDataType() {
        return SchemaDataType.OBJECT.equals(responseDataType) ? SchemaDataType.OBJECT : SchemaDataType.ARRAY;
    }

    public boolean hasResponseMessageId() {
        return responseMessageId != null && !responseMessageId.isBlank();
    }

    public void updateResponse(
            ResponseType responseType,
            String responseMessageId,
            SchemaDataType responseDataType,
            String fileMessageId,
            String fileContentKey,
            FileContentEncoding fileContentEncoding,
            String fileNameKey,
            String fileName,
            String fileContentTypeKey
    ) {
        this.responseType = responseType;
        this.responseMessageId = responseMessageId;
        this.responseDataType = responseDataType;
        this.fileMessageId = fileMessageId;
        this.fileContentKey = fileContentKey;
        this.fileContentEncoding = fileContentEncoding;
        this.fileNameKey = fileNameKey;
        this.fileName = fileName;
        this.fileContentTypeKey = fileContentTypeKey;
    }

    public boolean hasBody() {
        return bodyDataType != null && httpMethod != null && httpMethod.allowsBody();
    }

    public void update(
            String apiCode,
            String displayName,
            HttpMethodType httpMethod,
            String path,
            String workflowCode,
            SchemaDataType bodyDataType,
            String bodyMessageId,
            String description,
            Boolean enabled
    ) {
        this.apiCode = apiCode;
        this.displayName = displayName;
        this.httpMethod = httpMethod;
        this.path = path;
        this.workflowCode = workflowCode;
        this.bodyDataType = bodyDataType;
        this.bodyMessageId = bodyMessageId;
        this.description = description;
        this.enabled = enabled;
    }
}
