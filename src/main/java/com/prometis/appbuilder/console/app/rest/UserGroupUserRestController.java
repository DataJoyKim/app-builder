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

@RestController("console.UserGroupUserRestController")
@RequestMapping("/{applicationId}/console/api/user-group/user")
public class UserGroupUserRestController {
    @Autowired
    private UserGroupUserRepository repository;
    @Autowired
    private UserGroupRepository userGroupRepository;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{userGroupId}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("userGroupId") Long userGroupId) {
        List<UserGroupUser> results = repository.findByUserGroupId(userGroupId).stream()
                .filter(owned -> owned.getUserGroup() != null && applicationId.equals(owned.getUserGroup().getApplicationId()))
                .toList();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        Long userGroupId = Long.valueOf((String) params.get("userGroupId"));
        Long userId = Long.valueOf((String) params.get("userId"));

        UserGroup userGroup = userGroupRepository.findById(userGroupId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        User user = userRepository.findById(userId)
                .orElseThrow(RuntimeException::new);

        UserGroupUser createdData = UserGroupUser.builder()
                .user(user)
                .userGroup(userGroup)
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        UserGroupUser savedData = repository.findById(id)
                .filter(owned -> owned.getUserGroup() != null && applicationId.equals(owned.getUserGroup().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        Long userGroupId = Long.valueOf((String) params.get("userGroupId"));
        Long userId = Long.valueOf((String) params.get("userId"));

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
}
