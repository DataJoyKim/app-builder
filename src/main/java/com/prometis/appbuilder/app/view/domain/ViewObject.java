package com.prometis.appbuilder.app.view.domain;

import com.prometis.appbuilder.app.view.code.ObjectType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="VIEW_OBJECT_UQ",columnNames={"applicationId","objectCode"})})
@Entity
public class ViewObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;
    @Column(nullable = false, length = 100)
    private String objectCode;
    @Column(nullable = false, length = 200)
    private String objectName;
    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private ObjectType type;
    @Column(length = 100)
    private String path;
    @Column
    private Boolean useAuthValidation;
    // 기존 데이터 보존을 위해 DB 컬럼명은 그대로 둔다
    @Column(name = "USE_AUTHORITY_VALIDATION")
    private Boolean usePermissionValidation;
    @Column
    private Boolean useToolbar;

    public void update(
            String objectCode,
            String objectName,
            ObjectType type,
            String path,
            Boolean useAuthValidation,
            Boolean usePermissionValidation,
            Boolean useToolbar
    ) {
        this.objectCode = objectCode;
        this.objectName = objectName;
        this.type = type;
        this.path = path;
        this.useAuthValidation = useAuthValidation;
        this.usePermissionValidation = usePermissionValidation;
        this.useToolbar = useToolbar;
    }
}
