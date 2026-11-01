package com.prometis.appbuilder.app.workflow;

import jakarta.persistence.*;
import lombok.*;

import java.util.regex.Pattern;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="WORKFLOW_UQ",columnNames={"APPLICATION_ID","WORKFLOW_CODE"})})
@Entity
public class Workflow {
    /**
     * 워크플로우 코드 형식: 영문으로 시작하고 영문·숫자·_ 만, 100자 이하.
     * 코드는 화면/Rest API가 워크플로우를 부르는 이름이고 배포 DSL의 파일 이름도 되므로
     * 생성할 때 형식을 검사하고, 생성 후에는 바꾸지 않는다(update가 코드를 받지 않는다).
     */
    public static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,99}$");

    public static boolean isValidCode(String workflowCode) {
        return workflowCode != null && CODE_PATTERN.matcher(workflowCode).matches();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column(nullable = false, length = 100)
    private String workflowCode;

    @Column(nullable = false, length = 200)
    private String displayName;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private Boolean useAuthValidation;

    public void update(String displayName, String note, Boolean useAuthValidation) {
        this.displayName = displayName;
        this.note = note;
        this.useAuthValidation = useAuthValidation;
    }
}
