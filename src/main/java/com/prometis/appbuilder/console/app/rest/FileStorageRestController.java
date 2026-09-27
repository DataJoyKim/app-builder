package com.prometis.appbuilder.console.app.rest;

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
@RequestMapping("/{applicationId}/console/api/datasource/file-storage")
public class FileStorageRestController {
    @Autowired
    private DataSourceFileStorageRepository repository;
    @Autowired
    private DataSourceFileStorageValidator dataSourceFileStorageValidator;

    @GetMapping("")
    public ResponseEntity<?> getDataSource(@PathVariable("applicationId") String applicationId) {
        List<DataSourceFileStorage> results = repository.findByApplicationId(applicationId);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDataSource(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        DataSourceFileStorage results = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        DataSourceFileStorage createData = DataSourceFileStorage.builder()
                .applicationId(applicationId)
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
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id,
            @RequestBody Map<String,Object> params
    ) {
        DataSourceFileStorage metadata = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
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
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id
    ) {
        DataSourceFileStorage dataSource = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(dataSource.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<?> refreshDataSource(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id
    ) throws FileStorageCreationException {
        DataSourceFileStorage metadata = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        DataSourceFileStorageRegister.registry(metadata);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @GetMapping("/{id}/connect-valid")
    public ResponseEntity<?> validConnectDataSource(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id
    ) {
        DataSourceFileStorage metadata = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        ConnectValidation validate = dataSourceFileStorageValidator.validateConnect(metadata, DataSourceFileStorageRegister.getDataSourceMap());

        return new ResponseEntity<>(validate, HttpStatus.OK);
    }
}
