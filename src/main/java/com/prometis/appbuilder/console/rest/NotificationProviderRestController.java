package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.datasource.ConnectValidation;
import com.prometis.appbuilder.app.datasource.notification.*;
import com.prometis.appbuilder.app.executor.notification.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController("console.NotificationProviderRestController")
@RequestMapping("/console/api/datasource/notification")
public class NotificationProviderRestController {
    @Autowired
    private NotificationProviderRepository repository;
    @Autowired
    private NotificationValidator notificationValidator;

    @GetMapping("")
    public ResponseEntity<?> getDataSource() {
        List<NotificationProvider> results = repository.findAll();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDataSource(@PathVariable("id") Long id) {
        NotificationProvider results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> params) {
        NotificationProvider createData = NotificationProvider.builder()
                .dataSourceName((String) params.get("dataSourceName"))
                .displayName((String) params.get("displayName"))
                .note( (String) params.get("note"))
                .type(NotificationType.valueOf((String) params.get("type")))
                .options((String) params.get("options"))
                .build();

        NotificationProvider resultData = repository.save(createData);

        return new ResponseEntity<>(resultData, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRestServer(
            @PathVariable("id") Long id,
            @RequestBody Map<String,Object> params
    ) {
        NotificationProvider metadata = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        metadata.update(
                (String) params.get("dataSourceName"),
                (String) params.get("displayName"),
                (String) params.get("note"),
                NotificationType.valueOf((String) params.get("type")),
                (String) params.get("options")
        );

        NotificationProvider resultData = repository.save(metadata);

        return new ResponseEntity<>(resultData, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable("id") Long id
    ) {
        NotificationProvider dataSource = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(dataSource.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<?> refreshDataSource(
            @PathVariable("id") Long id
    ) throws NotificationCreationException {
        NotificationProvider metadata = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        DataSourceNotificationRegister.registry(metadata);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @GetMapping("/{id}/connect-valid")
    public ResponseEntity<?> validConnectDataSource(
            @PathVariable("id") Long id
    ) {
        NotificationProvider metadata = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        ConnectValidation validate = notificationValidator.validateConnect(metadata, DataSourceNotificationRegister.getDataSourceMap());

        return new ResponseEntity<>(validate, HttpStatus.OK);
    }
}
