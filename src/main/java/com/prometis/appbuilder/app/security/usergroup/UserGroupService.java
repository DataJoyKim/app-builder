package com.prometis.appbuilder.app.security.usergroup;

import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserGroupService {
    private final UserGroupUserRepository userGroupUserRepository;
    private final AppUserRepository appUserRepository;

    public List<UserGroupUser> getUserGroupUser(Long userId) {
        return userGroupUserRepository.findByUserId(userId);
    }

    /**
     * applicationId 애플리케이션에서 이 사용자에게 적용되는 사용자 그룹.
     * 사용자가 직접 매핑된 그룹과 "모든 사용자"가 매핑된 그룹을 합치고, 같은 그룹은 한 번만 넣는다.
     * "모든 사용자"는 이 애플리케이션의 사용자(AppUser)만 뜻하므로, 애플리케이션 사용자가 아니면 직접 매핑된 그룹만 준다.
     */
    public List<UserGroup> getAppliedUserGroups(String applicationId, Long userId) {
        Stream<UserGroupUser> mappings = userGroupUserRepository.findByUserIdAndUserGroupApplicationId(userId, applicationId).stream();

        if(appUserRepository.findByApplicationIdAndUserId(applicationId, userId).isPresent()) {
            mappings = Stream.concat(mappings,
                    userGroupUserRepository.findByAllUsersTrueAndUserGroupApplicationId(applicationId).stream());
        }

        Map<Long, UserGroup> groups = new LinkedHashMap<>();
        mappings.map(UserGroupUser::getUserGroup)
                .filter(Objects::nonNull)
                .forEach(group -> groups.putIfAbsent(group.getId(), group));

        return new ArrayList<>(groups.values());
    }
}
