package com.prometis.appbuilder.app.security.usergroup;

import com.prometis.appbuilder.platform.user.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class UserGroupUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "USER_ID")
    private User user;

    @ManyToOne
    @JoinColumn(name = "USER_GROUP_CODE")
    private UserGroup userGroup;

    // "모든 사용자" 매핑. user 없이 이 그룹을 애플리케이션의 모든 사용자에게 적용한다 (그룹당 하나).
    // 이 컬럼이 생기기 전의 행은 null 이고 개별 사용자 매핑이다
    @Column
    private Boolean allUsers;

    public boolean isAllUsers() {
        return Boolean.TRUE.equals(allUsers);
    }

    public void update(UserGroup userGroup, User user) {
        this.userGroup = userGroup;
        this.user = user;
    }
}
