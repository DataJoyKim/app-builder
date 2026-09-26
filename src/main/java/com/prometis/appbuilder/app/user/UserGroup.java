package com.prometis.appbuilder.app.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="USER_UQ",columnNames={"code"})})
@Entity
public class UserGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_USER_GROUP_ID")
    private UserGroup parentUserGroup;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "parentUserGroup", cascade = CascadeType.ALL)
    private List<UserGroup> children = new ArrayList<>();

    public void update(String code, String name, UserGroup parentUserGroup) {
        this.code = code;
        this.name = name;
        this.parentUserGroup = parentUserGroup;
    }

    @JsonIgnore
    public boolean isLeafNode() {
        return children.isEmpty();
    }

    @JsonIgnore
    public List<UserGroup> getAncestors() {
        List<UserGroup> ancestors = new ArrayList<>();

        UserGroup current = this.parentUserGroup;
        while (current != null) {
            ancestors.add(current);
            current = current.getParentUserGroup();
        }

        return ancestors;
    }
}
