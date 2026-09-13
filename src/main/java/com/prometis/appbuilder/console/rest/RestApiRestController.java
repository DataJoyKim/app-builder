package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.restapi.RestApi;
import com.prometis.appbuilder.restapi.RestApiDocumentService;
import com.prometis.appbuilder.restapi.RestApiParameterRepository;
import com.prometis.appbuilder.restapi.RestApiRepository;
import com.prometis.appbuilder.restapi.RestApiService;
import com.prometis.appbuilder.restapi.RestApiValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController("console.RestApiRestController")
@RequestMapping("/console/api/rest-api")
public class RestApiRestController {
    @Autowired
    private RestApiRepository repository;
    @Autowired
    private RestApiParameterRepository restApiParameterRepository;
    @Autowired
    private RestApiService restApiService;
    @Autowired
    private RestApiDocumentService restApiDocumentService;

    @GetMapping("")
    public ResponseEntity<?> getList() {
        List<RestApi> results = repository.findAll();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    // 응답 설정에서 고를 수 있는 워크플로우의 응답메시지ID 목록.
    @GetMapping("/workflow-response-message-ids")
    public ResponseEntity<?> workflowResponseMessageIds(@RequestParam("workflowCode") String workflowCode) {
        return new ResponseEntity<>(restApiDocumentService.responseMessageIdsOf(workflowCode), HttpStatus.OK);
    }

    // API 문서 화면용. 모든 API 의 정의, 파라미터 스키마, 감싼 워크플로우의 응답/오류 정보를 한 번에 돌려준다.
    @GetMapping("/document")
    public ResponseEntity<?> document() {
        return new ResponseEntity<>(restApiDocumentService.document(), HttpStatus.OK);
    }

    // API 기본정보와 파라미터 정의를 한 번에 돌려준다. 화면은 이 둘을 같이 편집하고 같이 저장한다.
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        RestApi restApi = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("restApi", restApi);
        results.put("parameters", restApiParameterRepository.findByRestApiIdOrderByOrderNum(id));

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody Map<String, Object> params) {
        try {
            return new ResponseEntity<>(restApiService.save(params), HttpStatus.OK);
        }
        catch (RestApiValidationException e) {
            return new ResponseEntity<>(Collections.singletonMap("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        restApiService.delete(id);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
