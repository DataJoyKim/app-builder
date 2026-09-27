package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.view.ViewObjectRepository;
import com.prometis.appbuilder.app.view.code.ObjectType;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("console.ViewObjectRestController")
@RequestMapping("/{applicationId}/console/api/object")
public class ViewObjectRestController {
    @Autowired
    private ViewObjectRepository repository;

    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId, @RequestParam(name = "objectCode", required = false) String objectCode
    ) {
        List<ViewObject> results;
        if(objectCode != null) {
            Optional<ViewObject> object = repository.findByApplicationIdAndObjectCode(applicationId, objectCode);
            results = new ArrayList<>();
            object.ifPresent(results::add);
        }
        else {
            results = repository.findByApplicationId(applicationId);
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        ViewObject results = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {

        ViewObject createdData = ViewObject.builder()
                .applicationId(applicationId)
                .objectCode((String) params.get("objectCode"))
                .objectName((String) params.get("objectName"))
                .type(ObjectType.valueOf((String) params.get("type")))
                .path((String) params.get("path"))
                .useAuthValidation(Boolean.valueOf((String) params.get("useAuthValidation")))
                .usePermissionValidation(Boolean.valueOf((String) params.get("usePermissionValidation")))
                .useToolbar(Boolean.valueOf((String) params.get("useToolbar")))
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        ViewObject savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        savedData.update(
                (String) params.get("objectCode"),
                (String) params.get("objectName"),
                ObjectType.valueOf((String) params.get("type")),
                (String) params.get("path"),
                Boolean.valueOf((String) params.get("useAuthValidation")),
                Boolean.valueOf((String) params.get("usePermissionValidation")),
                Boolean.valueOf((String) params.get("useToolbar"))
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        ViewObject savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
