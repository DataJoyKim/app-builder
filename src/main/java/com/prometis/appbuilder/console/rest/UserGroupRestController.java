package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.user.UserGroup;
import com.prometis.appbuilder.app.user.UserGroupRepository;
import com.prometis.appbuilder.app.user.dto.UserGroupDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("console.UserGroupRestController")
@RequestMapping("/console/api/user-group")
public class UserGroupRestController {
    @Autowired
    private UserGroupRepository repository;

    @GetMapping("")
    public ResponseEntity<?> getList() {
        List<UserGroup> results = repository.findAll();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/tree")
    public ResponseEntity<?> getTree() {
        List<UserGroupDto> results = UserGroupDto.of(repository.findAllTree());

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        UserGroup results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> params) {
        UserGroup parentUserGroup = resolveParentUserGroup(params);

        UserGroup createdData = UserGroup.builder()
                .code((String) params.get("code"))
                .name((String) params.get("name"))
                .parentUserGroup(parentUserGroup)
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        UserGroup savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        UserGroup parentUserGroup = resolveParentUserGroup(params);

        savedData.update(
                (String) params.get("code"),
                (String) params.get("name"),
                parentUserGroup
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        UserGroup savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private UserGroup resolveParentUserGroup(Map<String,Object> params) {
        String parentUserGroupCode = (String) params.get("parentUserGroupCode");
        if(parentUserGroupCode == null || parentUserGroupCode.isBlank()) {
            return null;
        }

        Optional<UserGroup> parentUserGroupOptional = repository.findByCode(parentUserGroupCode);

        return parentUserGroupOptional.orElse(null);
    }
}
