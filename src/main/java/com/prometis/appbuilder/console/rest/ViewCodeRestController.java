package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.view.ViewCodeRepository;
import com.prometis.appbuilder.app.view.domain.ViewCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController("console.ViewCodeRestController")
@RequestMapping("/console/api/object/code")
public class ViewCodeRestController {
    @Autowired
    private ViewCodeRepository repository;

    @GetMapping("")
    public ResponseEntity<?> getList() {
        List<ViewCode> results = repository.findAll();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        ViewCode results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> params) {

        ViewCode createdData = ViewCode.builder()
                .objectCode((String) params.get("objectCode"))
                .name((String) params.get("name"))
                .target((String) params.get("target"))
                .type((String) params.get("type"))
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        ViewCode savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        savedData.update(
                (String) params.get("objectCode"),
                (String) params.get("name"),
                (String) params.get("target"),
                (String) params.get("type")
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        ViewCode savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
