package com.prometis.appbuilder.app.view.domain;

import com.prometis.appbuilder.app.view.code.ObjectType;
import jakarta.persistence.*;
import lombok.*;

import java.util.regex.Pattern;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="VIEW_OBJECT_UQ",columnNames={"applicationId","objectCode"})})
@Entity
public class ViewObject {
    // 오브젝트코드는 화면 주소(/pages/{objectCode})와 FILE 소스 파일명({objectCode}.html)에 그대로 쓰인다
    public static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,99}$");
    public static final String CODE_RULE = "영문/숫자로 시작하는 영문, 숫자, _, - 100자 이내";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;
    // 만든 뒤에는 바꾸지 않는다 (화면 내용, 액션, FILE 소스가 이 코드로 묶여 있다)
    @Column(nullable = false, length = 100)
    private String objectCode;
    @Column(nullable = false, length = 200)
    private String objectName;
    // 만든 뒤에는 바꾸지 않는다. VIEW_BUILDER 는 ViewObjectContent 의 화면빌더 JSON, FILE 은 ViewSourceStorage 의 HTML 파일이 본문이다
    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private ObjectType type;
    @Column
    private Boolean useAuthValidation;
    // 기존 데이터 보존을 위해 DB 컬럼명은 그대로 둔다
    @Column(name = "USE_AUTHORITY_VALIDATION")
    private Boolean usePermissionValidation;
    @Column
    private Boolean useToolbar;

    public static boolean isValidCode(String objectCode) {
        return objectCode != null && CODE_PATTERN.matcher(objectCode).matches();
    }

    public void update(
            String objectName,
            Boolean useAuthValidation,
            Boolean usePermissionValidation,
            Boolean useToolbar
    ) {
        this.objectName = objectName;
        this.useAuthValidation = useAuthValidation;
        this.usePermissionValidation = usePermissionValidation;
        this.useToolbar = useToolbar;
    }
}
