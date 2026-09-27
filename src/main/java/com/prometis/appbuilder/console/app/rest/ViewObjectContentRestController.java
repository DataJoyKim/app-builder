package com.prometis.appbuilder.console.app.rest;

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
@RequestMapping("/{applicationId}/console/api/object-content")
public class ViewObjectContentRestController {
    @Autowired
    private ViewObjectContentRepository repository;

    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId, @RequestParam("objectCode") String objectCode) {
        List<ViewObjectContent> results = new ArrayList<>();

        if(objectCode != null && !objectCode.isEmpty()) {
             Optional<ViewObjectContent> viewObjectContentOptional = repository.findByApplicationIdAndObjectCode(applicationId, objectCode);
             if(viewObjectContentOptional.isPresent()) {
                 results.add(viewObjectContentOptional.get());
             }
        }
        else {
            results = repository.findByApplicationId(applicationId);
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        ViewObjectContent results = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("/upsert")
    public ResponseEntity<?> upsert(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        Optional<ViewObjectContent> optionalData = repository.findByApplicationIdAndObjectCode(applicationId, (String) params.get("objectCode"));
        ViewObjectContent data;
        if(optionalData.isPresent()) {
            data = optionalData.get();
            data.update(
                    (String) params.get("content")
            );
        }
        else {
            data = ViewObjectContent.builder()
                    .applicationId(applicationId)
                    .objectCode((String) params.get("objectCode"))
                    .content((String) params.get("content"))
                    .build();
        }

        return new ResponseEntity<>(repository.save(data), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {

        ViewObjectContent createdData = ViewObjectContent.builder()
                .applicationId(applicationId)
                .objectCode((String) params.get("objectCode"))
                .content((String) params.get("content"))
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        ViewObjectContent savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        savedData.update(
                (String) params.get("content")
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        ViewObjectContent savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
