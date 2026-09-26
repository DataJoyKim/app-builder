package com.prometis.appbuilder.app.user.dto;

import com.prometis.appbuilder.app.user.UserGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter @AllArgsConstructor @Builder
public class UserGroupDto {
    private Long id;
    private String code;
    private String name;
    private String parentUserGroupCode;
    private Boolean isLeafNode;
    private List<UserGroupDto> children;

    public static List<UserGroupDto> of(List<UserGroup> userGroupList) {
        List<UserGroupDto> result = new ArrayList<>();
        for (UserGroup userGroup : userGroupList) {
            result.add(of(userGroup));
        }

        return result;
    }

    public static UserGroupDto of(UserGroup userGroup) {
        if (userGroup == null) {
            return null;
        }

        UserGroup parentUserGroup = userGroup.getParentUserGroup();
        String parentUserGroupCode = (parentUserGroup != null) ? parentUserGroup.getCode() : null;

        List<UserGroupDto> children = new ArrayList<>();
        if (userGroup.getChildren() != null) {
            for (UserGroup child : userGroup.getChildren()) {
                children.add(of(child));
            }
        }

        return UserGroupDto.builder()
                .id(userGroup.getId())
                .code(userGroup.getCode())
                .name(userGroup.getName())
                .parentUserGroupCode(parentUserGroupCode)
                .isLeafNode(userGroup.isLeafNode())
                .children(children)
                .build();
    }
}
