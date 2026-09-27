package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.view.ViewActionRepository;
import com.prometis.appbuilder.app.view.domain.ViewAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController("console.ViewActionRestController")
@RequestMapping("/{applicationId}/console/api/action")
public class ViewActionRestController {
    @Autowired
    private ViewActionRepository repository;

    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId, @RequestParam("objectCode") String objectCode) {
        List<ViewAction> viewActions = repository.findByApplicationIdAndObjectCode(applicationId, objectCode);

        return new ResponseEntity<>(viewActions, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        ViewAction results = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("/upsert")
    public ResponseEntity<?> upsert(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        Object idObj = params.get("id");
        ViewAction data;
        if(idObj != null) {
            data = repository.findById(Long.valueOf((String) params.get("id")))
                                    .filter(owned -> applicationId.equals(owned.getApplicationId()))
                                    .orElseThrow();
            data.update(
                    (String) params.get("objectCode"),
                    (String) params.get("actionName"),
                    (String) params.get("displayName"),
                    (String) params.get("type"),
                    (String) params.get("argsName"),
                    (String) params.get("contents"),
                    (String) params.get("script")
            );
        }
        else {
            data = ViewAction.builder()
                    .applicationId(applicationId)
                    .objectCode((String) params.get("objectCode"))
                    .actionName((String) params.get("actionName"))
                    .displayName((String) params.get("displayName"))
                    .type((String) params.get("type"))
                    .argsName((String) params.get("argsName"))
                    .contents((String) params.get("contents"))
                    .script((String) params.get("script"))
                    .build();
        }

        return new ResponseEntity<>(repository.save(data), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        ViewAction savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
