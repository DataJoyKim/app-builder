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

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        Long userGroupId = Long.valueOf((String) params.get("userGroupId"));
        Long permissionId = Long.valueOf((String) params.get("permissionId"));

        UserGroup userGroup = userGroupRepository.findById(userGroupId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        Permission permission = permissionRepository.findById(permissionId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

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

        Long userGroupId = Long.valueOf((String) params.get("userGroupId"));
        Long permissionId = Long.valueOf((String) params.get("permissionId"));

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
}
