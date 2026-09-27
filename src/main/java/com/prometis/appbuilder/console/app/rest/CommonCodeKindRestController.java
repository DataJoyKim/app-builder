package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.code.CommonCodeKind;
import com.prometis.appbuilder.app.code.CommonCodeKindRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController("console.CommonCodeKindRestController")
@RequestMapping("/{applicationId}/console/api/common-code-kind")
public class CommonCodeKindRestController {
    @Autowired
    private CommonCodeKindRepository repository;

    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId) {
        List<CommonCodeKind> results = repository.findByApplicationId(applicationId);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        CommonCodeKind results = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {

        CommonCodeKind createdData = CommonCodeKind.builder()
                .applicationId(applicationId)
                .code((String) params.get("code"))
                .name((String) params.get("name"))
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        CommonCodeKind savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        savedData.update(
                (String) params.get("code"),
                (String) params.get("name")
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        CommonCodeKind savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
