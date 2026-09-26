package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.view.LayoutRepository;
import com.prometis.appbuilder.app.view.domain.Layout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController("console.LayoutRestController")
@RequestMapping("/console/api/layout")
public class LayoutRestController {
    @Autowired
    private LayoutRepository repository;

    @GetMapping("")
    public ResponseEntity<?> getList() {
        List<Layout> results = repository.findAll();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        Layout results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> params) {

        Layout createdData = Layout.builder()
                .useAuthValidation(Boolean.valueOf((String) params.get("useAuthValidation")))
                .useProfile(Boolean.valueOf((String) params.get("useProfile")))
                .useLogo(Boolean.valueOf((String) params.get("useLogo")))
                .logoText((String) params.get("logoText"))
                .logoBackgroundColor((String) params.get("logoBackgroundColor"))
                .logoLink((String) params.get("logoLink"))
                .logoImg((String) params.get("logoImg"))
                .layoutTitle((String) params.get("layoutTitle"))
                .homeObjectCode((String) params.get("homeObjectCode"))
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        Layout savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        savedData.update(
                Boolean.valueOf((String) params.get("useAuthValidation")),
                Boolean.valueOf((String) params.get("useProfile")),
                Boolean.valueOf((String) params.get("useLogo")),
                (String) params.get("logoText"),
                (String) params.get("logoBackgroundColor"),
                (String) params.get("logoLink"),
                (String) params.get("logoImg"),
                (String) params.get("layoutTitle"),
                (String) params.get("homeObjectCode")
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        Layout savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
