package com.prometis.appbuilder.app.security.usergroup;

import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserGroupPermissionRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserGroupUserRepository userGroupUserRepository;

    @Test
    void testFindByUserId() {
        // given
        User user = User.builder()
                .loginId("ks13ny")
                .userName("김낙영")
                .password("rlaskrdud1!")
                .email("ks13ny@netmarble.com")
                .build();

        user = userRepository.save(user);

        UserGroup group = UserGroup.builder()
                .applicationId("ehr")
                .code("test_user_group")
                .name("test_user_group")
                .build();

        group = userGroupRepository.save(group);

        UserGroupUser ugu = UserGroupUser.builder()
                .userGroup(group)
                .user(user)
                .build();

        userGroupUserRepository.save(ugu);

        // when
        List<UserGroupUser> results = userGroupUserRepository.findByUserId(user.getId());

        // then
        assertNotNull(results);
        assertFalse(results.isEmpty());

        for(UserGroupUser r : results) {
            assertNotNull(r.getUserGroup());
            assertNotNull(r.getUser());
            assertEquals("ks13ny", r.getUser().getLoginId());
            assertEquals("test_user_group", r.getUserGroup().getCode());
        }
    }
}