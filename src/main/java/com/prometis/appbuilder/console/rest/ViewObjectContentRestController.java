package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.view.ViewObjectContentRepository;
import com.prometis.appbuilder.app.view.domain.ViewObjectContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("console.ViewObjectContentRestController")
@RequestMapping("/console/api/object-content")
public class ViewObjectContentRestController {
    @Autowired
    private ViewObjectContentRepository repository;

    @GetMapping("")
    public ResponseEntity<?> getList(@RequestParam("objectCode") String objectCode) {
        List<ViewObjectContent> results = new ArrayList<>();

        if(objectCode != null && !objectCode.isEmpty()) {
             Optional<ViewObjectContent> viewObjectContentOptional = repository.findByObjectCode(objectCode);
             if(viewObjectContentOptional.isPresent()) {
                 results.add(viewObjectContentOptional.get());
             }
        }
        else {
            results = repository.findAll();
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        ViewObjectContent results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("/upsert")
    public ResponseEntity<?> upsert(@RequestBody Map<String,Object> params) {
        Optional<ViewObjectContent> optionalData = repository.findByObjectCode((String) params.get("objectCode"));
        ViewObjectContent data;
        if(optionalData.isPresent()) {
            data = optionalData.get();
            data.update(
                    (String) params.get("content")
            );
        }
        else {
            data = ViewObjectContent.builder()
                    .objectCode((String) params.get("objectCode"))
                    .content((String) params.get("content"))
                    .build();
        }

        return new ResponseEntity<>(repository.save(data), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> params) {

        ViewObjectContent createdData = ViewObjectContent.builder()
                .objectCode((String) params.get("objectCode"))
                .content((String) params.get("content"))
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        ViewObjectContent savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        savedData.update(
                (String) params.get("content")
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        ViewObjectContent savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
