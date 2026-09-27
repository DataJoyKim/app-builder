package com.prometis.appbuilder.app.security.usergroup;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserGroupService {
    private final UserGroupUserRepository userGroupUserRepository;

    public List<UserGroupUser> getUserGroupUser(Long userId) {
        return userGroupUserRepository.findByUserId(userId);
    }
}
