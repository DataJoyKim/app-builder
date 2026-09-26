package com.prometis.appbuilder.superadmin;

import com.prometis.appbuilder.app.security.domain.Authority;
import com.prometis.appbuilder.app.security.repository.AuthorityRepository;
import com.prometis.appbuilder.app.security.domain.UserGroupAuthority;
import com.prometis.appbuilder.app.security.repository.UserGroupAuthorityRepository;
import com.prometis.appbuilder.app.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SuperAdminService {
    private static final String SYS_ADMIN = "SYS_ADMIN";
    private final AuthorityRepository authorityRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupAuthorityRepository userGroupAuthorityRepository;
    private final UserGroupUserRepository userGroupUserRepository;
    private final UserRepository userRepository;

    public void createSysAdmin(String loginId) {
        Authority authority = Authority.builder()
                .code(SYS_ADMIN)
                .name(SYS_ADMIN)
                .build();

        Authority savedAuthority = authorityRepository.save(authority);

        UserGroup userGroup = UserGroup.builder()
                .code(SYS_ADMIN)
                .name(SYS_ADMIN)
                .build();

        UserGroup savedUserGroup = userGroupRepository.save(userGroup);

        UserGroupAuthority userGroupAuthority = UserGroupAuthority.builder()
                .authority(savedAuthority)
                .userGroup(savedUserGroup)
                .build();

        userGroupAuthorityRepository.save(userGroupAuthority);

        User user = userRepository.findByLoginId(loginId).orElseThrow();

        UserGroupUser userGroupUser = UserGroupUser.builder()
                .user(user)
                .userGroup(savedUserGroup)
                .build();

        userGroupUserRepository.save(userGroupUser);
    }
}
