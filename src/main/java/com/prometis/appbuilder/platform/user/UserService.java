package com.prometis.appbuilder.platform.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User getUserByLoginId(String loginId) {
        Optional<User> user = userRepository.findByLoginId(loginId);
        return user.orElse(null);

    }

    public User getUserByUserId(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.orElse(null);

    }
}
