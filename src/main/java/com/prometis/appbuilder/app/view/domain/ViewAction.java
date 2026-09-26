package com.prometis.appbuilder.app.view.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="VIEW_ACTION_UQ",columnNames={"objectCode","actionName"})})
@Entity
public class ViewAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String objectCode;
    @Column(nullable = false, length = 200)
    private String actionName;
    @Column(length = 200)
    private String displayName;
    @Column(length = 100)
    private String type;
    @Column(length = 200)
    private String argsName;
    @Lob
    @Column
    private String script;
    @Lob
    @Column
    private String contents;

    public void update(
            String objectCode,
            String actionName,
            String displayName,
            String type,
            String argsName,
            String contents,
            String script
    ) {
        this.objectCode = objectCode;
        this.actionName = actionName;
        this.displayName = displayName;
        this.argsName =argsName;
        this.type = type;
        this.contents = contents;
        this.script = script;
    }
}
