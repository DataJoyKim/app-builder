package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.security.usergroup.UserGroup;
import com.prometis.appbuilder.app.security.usergroup.UserGroupRepository;
import com.prometis.appbuilder.app.security.usergroup.dto.UserGroupDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("console.UserGroupRestController")
@RequestMapping("/{applicationId}/console/api/user-group")
public class UserGroupRestController {
    @Autowired
    private UserGroupRepository repository;

    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId) {
        List<UserGroup> results = repository.findByApplicationId(applicationId);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/tree")
    public ResponseEntity<?> getTree(@PathVariable("applicationId") String applicationId) {
        List<UserGroupDto> results = UserGroupDto.of(repository.findAllTree(applicationId));

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        UserGroup results = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        UserGroup parentUserGroup = resolveParentUserGroup(applicationId, params);

        UserGroup createdData = UserGroup.builder()
                .applicationId(applicationId)
                .code((String) params.get("code"))
                .name((String) params.get("name"))
                .parentUserGroup(parentUserGroup)
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        UserGroup savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        UserGroup parentUserGroup = resolveParentUserGroup(applicationId, params);

        savedData.update(
                (String) params.get("code"),
                (String) params.get("name"),
                parentUserGroup
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        UserGroup savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private UserGroup resolveParentUserGroup(String applicationId, Map<String,Object> params) {
        String parentUserGroupCode = (String) params.get("parentUserGroupCode");
        if(parentUserGroupCode == null || parentUserGroupCode.isBlank()) {
            return null;
        }

        Optional<UserGroup> parentUserGroupOptional = repository.findByApplicationIdAndCode(applicationId, parentUserGroupCode);

        return parentUserGroupOptional.orElse(null);
    }
}
