package com.prometis.appbuilder.console.rest;

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
@RequestMapping("/console/api/query")
public class QueryRestController {
    @Autowired
    private QueryRepository queryRepository;
    @Autowired
    private QueryService queryService;

    @GetMapping("")
    public ResponseEntity<?> getQuery(
            @RequestParam(name = "queryName", required = false) String queryName
    ) {
        List<Query> results;
        if(queryName != null) {
            Optional<Query> query = queryRepository.findByQueryName(queryName);
            results = new ArrayList<>();
            query.ifPresent(results::add);
        }
        else {
            results = queryRepository.findAll();
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getQuery(@PathVariable("id") Long id) {
        Query results = queryRepository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @Transactional
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody Map<String,Object> params) {
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
                    .queryName((String) pQuery.get("queryName"))
                    .displayName((String) pQuery.get("displayName"))
                    .dataSourceName((String) pQuery.get("dataSourceName"))
                    .query((String) pQuery.get("query"))
                    .queryParams(queryParams)
                    .build();
        }
        else {
            query = queryRepository.findById(id)
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
    public ResponseEntity<?> deleteQuery(@PathVariable("id") Long id) {
        Query query = queryRepository.findById(id)
                .orElseThrow(RuntimeException::new);

        queryRepository.deleteById(query.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/{queryName}/execute")
    public ResponseEntity<?> executeQuery(
            @PathVariable("queryName") String queryName,
            @RequestParam Map<String, Object> params
    ) {
        QueryRequest request = QueryRequest.builder()
                .contents(params)
                .build();

        QueryResult results = queryService.execute(queryName, request);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}
