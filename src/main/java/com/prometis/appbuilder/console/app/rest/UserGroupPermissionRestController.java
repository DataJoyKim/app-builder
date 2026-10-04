package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.security.permission.Permission;
import com.prometis.appbuilder.app.security.usergroup.UserGroupPermission;
import com.prometis.appbuilder.app.security.permission.PermissionRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroupPermissionRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroup;
import com.prometis.appbuilder.app.security.usergroup.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController("console.UserGroupPermissionRestController")
@RequestMapping("/{applicationId}/console/api/user-group/permission")
public class UserGroupPermissionRestController {
    @Autowired
    private UserGroupPermissionRepository repository;
    @Autowired
    private UserGroupRepository userGroupRepository;
    @Autowired
    private PermissionRepository permissionRepository;

    @GetMapping("/{userGroupId}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("userGroupId") Long userGroupId) {
        List<UserGroupPermission> results = repository.findByUserGroupId(userGroupId).stream()
                .filter(owned -> owned.getUserGroup() != null && applicationId.equals(owned.getUserGroup().getApplicationId()))
                .toList();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    // 권한 화면: 이 권한을 받은 사용자 그룹들
    @GetMapping("/by-permission/{permissionId}")
    public ResponseEntity<?> getByPermission(@PathVariable("applicationId") String applicationId, @PathVariable("permissionId") Long permissionId) {
        List<UserGroupPermission> results = repository.findByPermissionId(permissionId).stream()
                .filter(owned -> owned.getUserGroup() != null && applicationId.equals(owned.getUserGroup().getApplicationId()))
                .toList();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        Long userGroupId = idOf(params.get("userGroupId"));
        Long permissionId = idOf(params.get("permissionId"));

        UserGroup userGroup = userGroupRepository.findById(userGroupId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        Permission permission = permissionRepository.findById(permissionId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        // 같은 그룹에 같은 권한을 두 번 주지 않는다 (전파 여부는 기존 연결을 수정한다)
        if(repository.existsByUserGroupIdAndPermissionId(userGroupId, permissionId)) {
            return new ResponseEntity<>(Map.of("message", "이미 이 권한을 받은 사용자 그룹입니다."), HttpStatus.BAD_REQUEST);
        }

        Boolean lowerPermissionGrant = Boolean.TRUE.equals(params.get("lowerPermissionGrant"));

        UserGroupPermission createdData = UserGroupPermission.builder()
                .permission(permission)
                .userGroup(userGroup)
                .lowerPermissionGrant(lowerPermissionGrant)
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        UserGroupPermission savedData = repository.findById(id)
                .filter(owned -> owned.getUserGroup() != null && applicationId.equals(owned.getUserGroup().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        Long userGroupId = idOf(params.get("userGroupId"));
        Long permissionId = idOf(params.get("permissionId"));

        UserGroup userGroup = userGroupRepository.findById(userGroupId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        Permission permission = permissionRepository.findById(permissionId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        Boolean lowerPermissionGrant = Boolean.TRUE.equals(params.get("lowerPermissionGrant"));

        savedData.update(userGroup, permission, lowerPermissionGrant);

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        UserGroupPermission savedData = repository.findById(id)
                .filter(owned -> owned.getUserGroup() != null && applicationId.equals(owned.getUserGroup().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 화면에 따라 id 를 문자열("3") 또는 숫자(3)로 보낸다
    private static Long idOf(Object value) {
        return Long.valueOf(String.valueOf(value));
    }
}
