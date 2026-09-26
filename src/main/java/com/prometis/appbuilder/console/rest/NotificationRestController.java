package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.notification.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("console.NotificationRestController")
@RequestMapping("/console/api/notification")
public class NotificationRestController {
    @Autowired
    private NotificationRepository repository;
    @Autowired
    private NotificationService notificationService;

    @GetMapping("")
    public ResponseEntity<?> getNotification(
            @RequestParam(name = "notificationName", required = false) String notificationName
    ) {
        List<Notification> results;
        if(notificationName != null) {
            Optional<Notification> notification = repository.findByNotificationName(notificationName);
            results = new ArrayList<>();
            notification.ifPresent(results::add);
        }
        else {
            results = repository.findAll();
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNotification(@PathVariable("id") Long id) {
        Notification results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @Transactional
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody Map<String,Object> params) {

        Long id = (params.get("id") == null || ((String) params.get("id")).isEmpty())
                ? null
                : Long.valueOf((String) params.get("id"));

        Notification notification;
        if (id == null) {
            notification = Notification.builder()
                    .notificationName((String) params.get("notificationName"))
                    .displayName((String) params.get("displayName"))
                    .dataSourceName((String) params.get("dataSourceName"))
                    .subject((String) params.get("subject"))
                    .content((String) params.get("content"))
                    .toAlias((String) params.get("toAlias"))
                    .enableSend((Boolean) params.get("enableSend"))
                    .build();
        }
        else {
            notification = repository.findById(id)
                    .orElseThrow(RuntimeException::new);

            notification.update(
                    (String) params.get("notificationName"),
                    (String) params.get("displayName"),
                    (String) params.get("dataSourceName"),
                    (String) params.get("toAlias"),
                    (String) params.get("subject"),
                    (String) params.get("content"),
                    (Boolean) params.get("enableSend")
            );
        }

        Notification results = repository.save(notification);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable("id") Long id) {
        Notification notification = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(notification.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/{notificationName}/execute")
    public ResponseEntity<?> execute(
            @PathVariable("notificationName") String notificationName,
            @RequestParam Map<String, Object> params
    ) {
        NotificationRequest request = NotificationRequest.builder()
                .params(params)
                .build();

        NotificationResult results = notificationService.execute(notificationName, request);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}
