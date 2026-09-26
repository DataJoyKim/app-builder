package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.view.ViewActionRepository;
import com.prometis.appbuilder.app.view.domain.ViewAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController("console.ViewActionRestController")
@RequestMapping("/console/api/action")
public class ViewActionRestController {
    @Autowired
    private ViewActionRepository repository;

    @GetMapping("")
    public ResponseEntity<?> getList(@RequestParam("objectCode") String objectCode) {
        List<ViewAction> viewActions = repository.findByObjectCode(objectCode);

        return new ResponseEntity<>(viewActions, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        ViewAction results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("/upsert")
    public ResponseEntity<?> upsert(@RequestBody Map<String,Object> params) {
        Object idObj = params.get("id");
        ViewAction data;
        if(idObj != null) {
            data = repository.findById(Long.valueOf((String) params.get("id")))
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
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        ViewAction savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
