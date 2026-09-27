package com.prometis.appbuilder.console.platform.rest;

import com.prometis.appbuilder.console.platform.dto.ApplicationRequest;
import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationIdPolicy;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.application.ApplicationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/console/api/application")
public class ApplicationRestController {
    @Autowired
    private ApplicationRepository repository;
    @Autowired
    private ApplicationIdPolicy applicationIdPolicy;

    @GetMapping("")
    public ResponseEntity<?> getList() {
        List<Application> results = repository.findAll();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    // 화면에서 입력 중에 예약 경로를 바로 알려주기 위해 사용
    @GetMapping("/reserved-ids")
    public ResponseEntity<?> getReservedIds() {
        return new ResponseEntity<>(applicationIdPolicy.getReservedIds(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        Application results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> insert(@RequestBody ApplicationRequest request) {
        String error = validate(request, null);
        if(error != null) {
            return badRequest(error);
        }

        Application saved = repository.save(Application.builder()
                .applicationId(request.getApplicationId().trim())
                .name(request.getName().trim())
                .status(request.getStatus())
                .description(request.getDescription())
                .build());

        return new ResponseEntity<>(saved, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody ApplicationRequest request) {
        Application savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        String error = validate(request, id);
        if(error != null) {
            return badRequest(error);
        }

        savedData.update(
                request.getApplicationId().trim(),
                request.getName().trim(),
                request.getStatus(),
                request.getDescription()
        );

        repository.save(savedData);

        return new ResponseEntity<>(savedData, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        Application savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String validate(ApplicationRequest request, Long id) {
        String idError = applicationIdPolicy.validate(isBlank(request.getApplicationId()) ? null : request.getApplicationId().trim());
        if(idError != null) return idError;
        if(isBlank(request.getName())) return "애플리케이션명을 입력해주세요.";
        if(isBlank(request.getStatus())
                || Arrays.stream(ApplicationStatus.values()).noneMatch(s -> s.name().equals(request.getStatus()))) {
            return "상태를 선택해주세요.";
        }

        Optional<Application> sameId = repository.findByApplicationId(request.getApplicationId().trim());
        if(sameId.isPresent() && !sameId.get().getId().equals(id)) {
            return "이미 존재하는 애플리케이션ID입니다.";
        }

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ResponseEntity<?> badRequest(String message) {
        return new ResponseEntity<>(Map.of("message", message), HttpStatus.BAD_REQUEST);
    }
}
