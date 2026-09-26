package com.prometis.appbuilder.app.code;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="COMMON_CODE_UQ",columnNames={"CODE_KIND_CODE", "code"})})
@Entity
public class CommonCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 300)
    private String name;

    @Column
    private Integer orderNum;

    @ManyToOne
    @JoinColumn(name = "CODE_KIND_CODE")
    private CommonCodeKind codeKind;

    public void update(String code, String name, Integer orderNum, CommonCodeKind codeKind) {
        this.code = code;
        this.name = name;
        this.codeKind = codeKind;
        this.orderNum = orderNum;
    }

    public String getCommonCodeKindCode() {
        if(this.codeKind == null) {
            return null;
        }

        return this.codeKind.getCode();
    }
}
