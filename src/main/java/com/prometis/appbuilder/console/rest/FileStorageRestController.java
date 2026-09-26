package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.datasource.ConnectValidation;
import com.prometis.appbuilder.app.datasource.filestorage.*;
import com.prometis.appbuilder.app.executor.file.StorageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController("console.FileStorageRestController")
@RequestMapping("/console/api/datasource/file-storage")
public class FileStorageRestController {
    @Autowired
    private DataSourceFileStorageRepository repository;
    @Autowired
    private DataSourceFileStorageValidator dataSourceFileStorageValidator;

    @GetMapping("")
    public ResponseEntity<?> getDataSource() {
        List<DataSourceFileStorage> results = repository.findAll();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDataSource(@PathVariable("id") Long id) {
        DataSourceFileStorage results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> params) {
        DataSourceFileStorage createData = DataSourceFileStorage.builder()
                .dataSourceName((String) params.get("dataSourceName"))
                .displayName((String) params.get("displayName"))
                .note((String) params.get("note"))
                .storageType(StorageType.valueOf((String) params.get("storageType")))
                .options((String) params.get("options"))
                .build();

        DataSourceFileStorage resultData = repository.save(createData);

        return new ResponseEntity<>(resultData, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable("id") Long id,
            @RequestBody Map<String,Object> params
    ) {
        DataSourceFileStorage metadata = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        metadata.update(
                (String) params.get("dataSourceName"),
                (String) params.get("displayName"),
                (String) params.get("note"),
                StorageType.valueOf((String) params.get("storageType")),
                (String) params.get("options")
        );

        DataSourceFileStorage resultData = repository.save(metadata);

        return new ResponseEntity<>(resultData, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable("id") Long id
    ) {
        DataSourceFileStorage dataSource = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(dataSource.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<?> refreshDataSource(
            @PathVariable("id") Long id
    ) throws FileStorageCreationException {
        DataSourceFileStorage metadata = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        DataSourceFileStorageRegister.registry(metadata);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @GetMapping("/{id}/connect-valid")
    public ResponseEntity<?> validConnectDataSource(
            @PathVariable("id") Long id
    ) {
        DataSourceFileStorage metadata = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        ConnectValidation validate = dataSourceFileStorageValidator.validateConnect(metadata, DataSourceFileStorageRegister.getDataSourceMap());

        return new ResponseEntity<>(validate, HttpStatus.OK);
    }
}
