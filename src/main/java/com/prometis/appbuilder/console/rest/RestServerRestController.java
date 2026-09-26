package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.datasource.ConnectValidation;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServer;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServerRegister;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServerValidator;
import com.prometis.appbuilder.console.repository.ConsoleRestServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("console.RestServerRestController")
@RequestMapping("/console/api/datasource/rest-server")
public class RestServerRestController {
    @Autowired
    private ConsoleRestServerRepository restServerRepository;
    @Autowired
    private DataSourceRestServerValidator dataSourceRestServerValidator;

    @GetMapping("")
    public ResponseEntity<?> getDataSource() {
        List<DataSourceRestServer> results = restServerRepository.findAll();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDataSource(@PathVariable("id") Long id) {
        DataSourceRestServer results = restServerRepository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> createRestServer(@RequestBody Map<String,Object> params) {
        restServerRepository.insert(
                (String) params.get("dataSourceName"),
                (String) params.get("displayName"),
                (String) params.get("note"),
                (String) params.get("baseUrl"),
                Integer.valueOf((String) params.get("connectTimeout")),
                Integer.valueOf((String) params.get("connectRequestTimeout")),
                Integer.valueOf((String) params.get("connectionMaxTotal")),
                Integer.valueOf((String) params.get("connectionDefaultMaxPerRoute"))
        );

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRestServer(
            @PathVariable("id") Long id,
            @RequestBody Map<String,Object> params
    ) {
        DataSourceRestServer metadata = restServerRepository.findById(id)
                .orElseThrow(RuntimeException::new);

        restServerRepository.update(
                metadata.getId(),
                (String) params.get("dataSourceName"),
                (String) params.get("displayName"),
                (String) params.get("note"),
                (String) params.get("baseUrl"),
                Integer.valueOf((String) params.get("connectTimeout")),
                Integer.valueOf((String) params.get("connectRequestTimeout")),
                Integer.valueOf((String) params.get("connectionMaxTotal")),
                Integer.valueOf((String) params.get("connectionDefaultMaxPerRoute"))
        );

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRestServer(
            @PathVariable("id") Long id
    ) {
        DataSourceRestServer dataSource = restServerRepository.findById(id)
                .orElseThrow(RuntimeException::new);

        restServerRepository.deleteById(dataSource.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<?> refreshDataSource(
            @PathVariable("id") Long id
    ) {
        DataSourceRestServer metadata = restServerRepository.findById(id)
                .orElseThrow(RuntimeException::new);

        DataSourceRestServerRegister.registry(metadata);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }
    @GetMapping("/{id}/connect-valid")
    public ResponseEntity<?> validConnectDataSource(
            @PathVariable("id") Long id
    ) {
        DataSourceRestServer metadata = restServerRepository.findById(id)
                .orElseThrow(RuntimeException::new);

        ConnectValidation validate = dataSourceRestServerValidator.validateConnect(metadata, DataSourceRestServerRegister.getDataSourceMap());

        return new ResponseEntity<>(validate, HttpStatus.OK);
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getDataSourceSummary() {
        Map<String, Object> results = new HashMap<>();

        List<DataSourceRestServer> metadata = restServerRepository.findAll();

        results.put("metadata",metadata);
        results.put("dataSources", DataSourceRestServerRegister.getDataSourceMap());

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}
