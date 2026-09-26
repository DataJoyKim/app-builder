package com.prometis.appbuilder.app.user;

import com.prometis.core.crypto.PasswordEncoder;
import com.prometis.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserGroupUserRepository userGroupUserRepository;
    private final PasswordEncoder passwordEncoder;
    public User getUserByLoginId(String loginId) {
        Optional<User> user = userRepository.findByLoginId(loginId);
        if(user.isEmpty()){
            return null;
        }

        return user.get();
    }

    public User getUserByUserId(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            return null;
        }

        return user.get();
    }

    public List<UserGroupUser> getUserGroupUser(Long userId) {
        return userGroupUserRepository.findByUserId(userId);
    }

    public User signUp(SignUpRequest signUpRequest) throws BusinessException {
        Optional<User> savedUser = userRepository.findByLoginId(signUpRequest.getLoginId());
        if(savedUser.isPresent()){
            throw new BusinessException(UserErrorMessage.ALREADY_EXIST_USER);
        }

        User user = User.signUp(passwordEncoder, signUpRequest);

        return userRepository.save(user);
    }
}
