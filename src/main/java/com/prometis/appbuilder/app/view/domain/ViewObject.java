package com.prometis.appbuilder.app.view.domain;

import com.prometis.appbuilder.app.view.code.ObjectType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="VIEW_OBJECT_UQ",columnNames={"objectCode"})})
@Entity
public class ViewObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
    @Column
    private Boolean useAuthorityValidation;
    @Column
    private Boolean useToolbar;

    public void update(
            String objectCode,
            String objectName,
            ObjectType type,
            String path,
            Boolean useAuthValidation,
            Boolean useAuthorityValidation,
            Boolean useToolbar
    ) {
        this.objectCode = objectCode;
        this.objectName = objectName;
        this.type = type;
        this.path = path;
        this.useAuthValidation = useAuthValidation;
        this.useAuthorityValidation = useAuthorityValidation;
        this.useToolbar = useToolbar;
    }
}
