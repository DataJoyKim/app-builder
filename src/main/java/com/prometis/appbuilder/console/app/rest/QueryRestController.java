package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.query.*;
import com.prometis.appbuilder.app.query.code.AutoValueType;
import com.prometis.appbuilder.app.query.code.InOut;
import com.prometis.appbuilder.app.query.code.ParamType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("console.QueryRestController")
@RequestMapping("/{applicationId}/console/api/query")
public class QueryRestController {
    @Autowired
    private QueryRepository queryRepository;
    @Autowired
    private QueryService queryService;

    @GetMapping("")
    public ResponseEntity<?> getQuery(@PathVariable("applicationId") String applicationId, @RequestParam(name = "queryName", required = false) String queryName
    ) {
        List<Query> results;
        if(queryName != null) {
            Optional<Query> query = queryRepository.findByApplicationIdAndQueryName(applicationId, queryName);
            results = new ArrayList<>();
            query.ifPresent(results::add);
        }
        else {
            results = queryRepository.findByApplicationId(applicationId);
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getQuery(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        Query results = queryRepository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @Transactional
    @PostMapping("/save")
    public ResponseEntity<?> save(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        Map<String,Object> pQuery = (Map<String, Object>) params.get("query");
        List<Map<String,Object>> pQueryParams = (List<Map<String,Object>>) params.get("queryParams");

        Long id = (pQuery.get("id") == null || ((String) pQuery.get("id")).isEmpty())
                ? null
                : Long.valueOf((String) pQuery.get("id"));

        List<QueryParam> queryParams = new ArrayList<>();
        for(Map<String,Object> p : pQueryParams) {
            queryParams.add(
                    QueryParam.builder()
                        .paramName((String) p.get("paramName"))
                        .paramType(ParamType.valueOf((String) p.get("paramType")))
                        .autoValueType(AutoValueType.valueOf((String) p.get("autoValueType")))
                        .inOut(InOut.valueOf((String) p.get("inOut")))
                    .build());
        }

        Query query;
        if (id == null) {
            query = Query.builder()
                    .applicationId(applicationId)
                    .queryName((String) pQuery.get("queryName"))
                    .displayName((String) pQuery.get("displayName"))
                    .dataSourceName((String) pQuery.get("dataSourceName"))
                    .query((String) pQuery.get("query"))
                    .queryParams(queryParams)
                    .build();
        }
        else {
            query = queryRepository.findById(id)
                    .filter(owned -> applicationId.equals(owned.getApplicationId()))
                    .orElseThrow(RuntimeException::new);

            query.update(
                    (String) pQuery.get("queryName"),
                    (String) pQuery.get("displayName"),
                    (String) pQuery.get("dataSourceName"),
                    (String) pQuery.get("query"),
                    queryParams
            );
        }

        Query results = queryRepository.save(query);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuery(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        Query query = queryRepository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        queryRepository.deleteById(query.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/{queryName}/execute")
    public ResponseEntity<?> executeQuery(@PathVariable("applicationId") String applicationId, @PathVariable("queryName") String queryName,
            @RequestParam Map<String, Object> params
    ) {
        QueryRequest request = QueryRequest.builder()
                .contents(params)
                .build();

        QueryResult results = queryService.execute(applicationId, queryName, request);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}
