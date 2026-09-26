package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.security.domain.Authority;
import com.prometis.appbuilder.app.security.domain.UserGroupAuthority;
import com.prometis.appbuilder.app.security.repository.AuthorityRepository;
import com.prometis.appbuilder.app.security.repository.UserGroupAuthorityRepository;
import com.prometis.appbuilder.app.user.UserGroup;
import com.prometis.appbuilder.app.user.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController("console.UserGroupAuthorityRestController")
@RequestMapping("/console/api/user-group/authority")
public class UserGroupAuthorityRestController {
    @Autowired
    private UserGroupAuthorityRepository repository;
    @Autowired
    private UserGroupRepository userGroupRepository;
    @Autowired
    private AuthorityRepository authorityRepository;

    @GetMapping("/{userGroupId}")
    public ResponseEntity<?> get(@PathVariable("userGroupId") Long userGroupId) {
        List<UserGroupAuthority> results = repository.findByUserGroupId(userGroupId);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> params) {
        Long userGroupId = Long.valueOf((String) params.get("userGroupId"));
        Long authorityId = Long.valueOf((String) params.get("authorityId"));

        UserGroup userGroup = userGroupRepository.findById(userGroupId)
                .orElseThrow(RuntimeException::new);

        Authority authority = authorityRepository.findById(authorityId)
                .orElseThrow(RuntimeException::new);

        Boolean lowerAuthorityGrant = Boolean.TRUE.equals(params.get("lowerAuthorityGrant"));

        UserGroupAuthority createdData = UserGroupAuthority.builder()
                .authority(authority)
                .userGroup(userGroup)
                .lowerAuthorityGrant(lowerAuthorityGrant)
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        UserGroupAuthority savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        Long userGroupId = Long.valueOf((String) params.get("userGroupId"));
        Long authorityId = Long.valueOf((String) params.get("authorityId"));

        UserGroup userGroup = userGroupRepository.findById(userGroupId)
                .orElseThrow(RuntimeException::new);

        Authority authority = authorityRepository.findById(authorityId)
                .orElseThrow(RuntimeException::new);

        Boolean lowerAuthorityGrant = Boolean.TRUE.equals(params.get("lowerAuthorityGrant"));

        savedData.update(userGroup, authority, lowerAuthorityGrant);

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        UserGroupAuthority savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
