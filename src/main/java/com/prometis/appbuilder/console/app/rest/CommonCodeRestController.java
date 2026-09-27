package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.code.CommonCode;
import com.prometis.appbuilder.app.code.CommonCodeKind;
import com.prometis.appbuilder.app.code.CommonCodeKindRepository;
import com.prometis.appbuilder.app.code.CommonCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController("console.CommonCodeRestController")
@RequestMapping("/{applicationId}/console/api/common-code")
public class CommonCodeRestController {
    @Autowired
    private CommonCodeKindRepository commonCodeKindRepository;
    @Autowired
    private CommonCodeRepository repository;

    @GetMapping("")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @RequestParam Map<String,Object> params) {
        String codeKindCode = (String) params.get("codeKindCode");

        CommonCodeKind codeKind = commonCodeKindRepository.findByApplicationIdAndCode(applicationId, codeKindCode)
                                            .orElseThrow();

        List<CommonCode> results = repository.findByCodeKind(codeKind);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        String codeKindCode = (String) params.get("codeKindCode");

        CommonCodeKind codeKind = commonCodeKindRepository.findByApplicationIdAndCode(applicationId, codeKindCode)
                .orElseThrow();

        CommonCode createdData = CommonCode.builder()
                .code((String) params.get("code"))
                .name((String) params.get("name"))
                .codeKind(codeKind)
                .build();

        repository.save(createdData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        CommonCode savedData = repository.findById(id)
                .filter(owned -> owned.getCodeKind() != null && applicationId.equals(owned.getCodeKind().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        String codeKindCode = (String) params.get("codeKindCode");

        CommonCodeKind codeKind = commonCodeKindRepository.findByApplicationIdAndCode(applicationId, codeKindCode)
                .orElseThrow();

        savedData.update(
                (String) params.get("code"),
                (String) params.get("name"),
                Integer.parseInt((String) params.get("orderNum")),
                codeKind
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        CommonCode savedData = repository.findById(id)
                .filter(owned -> owned.getCodeKind() != null && applicationId.equals(owned.getCodeKind().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
