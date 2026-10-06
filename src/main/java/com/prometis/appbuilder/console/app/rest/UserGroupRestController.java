package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.security.company.CompanyRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroup;
import com.prometis.appbuilder.app.security.usergroup.UserGroupRepository;
import com.prometis.appbuilder.app.security.usergroup.dto.UserGroupDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자 그룹은 회사별로 관리한다. 코드는 (applicationId, companyCode, code) 로 유일하고,
 * 상위 그룹도 같은 회사 안에서만 고른다. 회사는 한 번 정하면 바꾸지 않는다.
 */
@RestController("console.UserGroupRestController")
@RequestMapping("/{applicationId}/console/api/user-group")
public class UserGroupRestController {
    @Autowired
    private UserGroupRepository repository;
    @Autowired
    private CompanyRepository companyRepository;

    // companyCode 가 없으면 애플리케이션의 모든 회사 그룹 (가입 기본 그룹 선택 등)
    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId,
                                     @RequestParam(value = "companyCode", required = false) String companyCode) {
        String company = text(companyCode);
        List<UserGroup> results = (company == null)
                ? repository.findByApplicationId(applicationId)
                : repository.findByApplicationIdAndCompanyCode(applicationId, company);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    // companyCode 가 없으면 모든 회사의 트리 (권한 화면에서 회사 구분 없이 그룹을 고를 때)
    @GetMapping("/tree")
    public ResponseEntity<?> getTree(@PathVariable("applicationId") String applicationId,
                                     @RequestParam(value = "companyCode", required = false) String companyCode) {
        String company = text(companyCode);
        List<UserGroup> roots = (company == null)
                ? repository.findAllTree(applicationId)
                : repository.findAllTree(applicationId, company);

        List<UserGroupDto> results = UserGroupDto.of(roots);

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
        String companyCode = text(params.get("companyCode"));
        String code = text(params.get("code"));
        String name = text(params.get("name"));

        if(companyCode == null) {
            return error("회사를 선택해주세요.");
        }
        if(companyRepository.findByApplicationIdAndCompanyCode(applicationId, companyCode).isEmpty()) {
            return error("등록되지 않은 회사입니다. [" + companyCode + "]");
        }
        if(code == null || name == null) {
            return error("사용자그룹 코드와 이름을 입력해주세요.");
        }
        if(repository.findByApplicationIdAndCompanyCodeAndCode(applicationId, companyCode, code).isPresent()) {
            return error("이미 존재하는 사용자그룹 코드입니다. [" + code + "]");
        }

        UserGroup parentUserGroup = resolveParentUserGroup(applicationId, companyCode, params);

        UserGroup createdData = UserGroup.builder()
                .applicationId(applicationId)
                .companyCode(companyCode)
                .code(code)
                .name(name)
                .parentUserGroup(parentUserGroup)
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        UserGroup savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        String companyCode = savedData.getCompanyCode();
        String code = text(params.get("code"));
        String name = text(params.get("name"));

        if(code == null || name == null) {
            return error("사용자그룹 코드와 이름을 입력해주세요.");
        }
        if(repository.findByApplicationIdAndCompanyCodeAndCode(applicationId, companyCode, code).filter(other -> !other.getId().equals(id)).isPresent()) {
            return error("이미 존재하는 사용자그룹 코드입니다. [" + code + "]");
        }

        UserGroup parentUserGroup = resolveParentUserGroup(applicationId, companyCode, params);
        if(parentUserGroup != null && parentUserGroup.getId().equals(id)) {
            return error("자기 자신을 상위 그룹으로 지정할 수 없습니다.");
        }

        savedData.update(code, name, parentUserGroup);

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

    // 상위 그룹은 같은 회사의 그룹만 인정한다
    private UserGroup resolveParentUserGroup(String applicationId, String companyCode, Map<String,Object> params) {
        String parentUserGroupCode = text(params.get("parentUserGroupCode"));
        if(parentUserGroupCode == null) {
            return null;
        }

        Optional<UserGroup> parentUserGroupOptional = repository.findByApplicationIdAndCompanyCodeAndCode(applicationId, companyCode, parentUserGroupCode);

        return parentUserGroupOptional.orElse(null);
    }

    private static String text(Object value) {
        if(value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();

        return text.isEmpty() ? null : text;
    }

    private static ResponseEntity<?> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
