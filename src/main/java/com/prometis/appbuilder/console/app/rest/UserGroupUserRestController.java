package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.security.usergroup.UserGroup;
import com.prometis.appbuilder.app.security.usergroup.UserGroupRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroupUser;
import com.prometis.appbuilder.app.security.usergroup.UserGroupUserRepository;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 콘솔 UserGroup 화면의 사용자 매핑. 개별 사용자 또는 "모든 사용자"(allUsers, 그룹당 하나)를 매핑한다.
 * "모든 사용자"가 매핑된 그룹은 그 애플리케이션의 모든 사용자에게 적용된다 (UserGroupService.getAppliedUserGroups).
 */
@RestController("console.UserGroupUserRestController")
@RequestMapping("/{applicationId}/console/api/user-group/user")
public class UserGroupUserRestController {
    @Autowired
    private UserGroupUserRepository repository;
    @Autowired
    private UserGroupRepository userGroupRepository;
    @Autowired
    private UserRepository userRepository;

    /**
     * 엔티티를 그대로 내보내면 User 의 비밀번호 해시까지 나가므로 화면에 필요한 값만 담는다.
     * "모든 사용자" 행은 allUsers=true 이고 사용자 값이 비어 있다.
     */
    public record UserGroupUserResponse(Long id, boolean allUsers, Long userId, String userName, String loginId) {
        static UserGroupUserResponse of(UserGroupUser mapping) {
            User user = mapping.getUser();
            return new UserGroupUserResponse(
                    mapping.getId(),
                    mapping.isAllUsers(),
                    user == null ? null : user.getId(),
                    user == null ? null : user.getUserName(),
                    user == null ? null : user.getLoginId());
        }
    }

    @GetMapping("/{userGroupId}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("userGroupId") Long userGroupId) {
        // "모든 사용자"를 맨 앞에 둔다
        List<UserGroupUserResponse> results = repository.findByUserGroupId(userGroupId).stream()
                .filter(owned -> owned.getUserGroup() != null && applicationId.equals(owned.getUserGroup().getApplicationId()))
                .sorted((a, b) -> Boolean.compare(b.isAllUsers(), a.isAllUsers()))
                .map(UserGroupUserResponse::of)
                .toList();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        Long userGroupId = Long.valueOf(String.valueOf(params.get("userGroupId")));

        UserGroup userGroup = userGroupRepository.findById(userGroupId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        if(Boolean.TRUE.equals(params.get("allUsers")) || "true".equals(params.get("allUsers"))) {
            if(repository.existsByUserGroupIdAndAllUsersTrue(userGroupId)) {
                return badRequest("이미 모든 사용자가 추가되어 있습니다.");
            }

            UserGroupUser saved = repository.save(UserGroupUser.builder()
                    .userGroup(userGroup)
                    .allUsers(true)
                    .build());
            return new ResponseEntity<>(UserGroupUserResponse.of(saved), HttpStatus.OK);
        }

        Long userId = Long.valueOf(String.valueOf(params.get("userId")));

        User user = userRepository.findById(userId)
                .orElseThrow(RuntimeException::new);

        UserGroupUser createdData = UserGroupUser.builder()
                .user(user)
                .userGroup(userGroup)
                .build();

        return new ResponseEntity<>(UserGroupUserResponse.of(repository.save(createdData)), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        UserGroupUser savedData = repository.findById(id)
                .filter(owned -> owned.getUserGroup() != null && applicationId.equals(owned.getUserGroup().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        // "모든 사용자" 행은 바꿀 사용자가 없다. 지우고 다시 추가한다
        if(savedData.isAllUsers()) {
            return badRequest("모든 사용자는 수정할 수 없습니다.");
        }

        Long userGroupId = Long.valueOf(String.valueOf(params.get("userGroupId")));
        Long userId = Long.valueOf(String.valueOf(params.get("userId")));

        UserGroup userGroup = userGroupRepository.findById(userGroupId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        User user = userRepository.findById(userId)
                .orElseThrow(RuntimeException::new);

        savedData.update(userGroup, user);

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        UserGroupUser savedData = repository.findById(id)
                .filter(owned -> owned.getUserGroup() != null && applicationId.equals(owned.getUserGroup().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private static ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
