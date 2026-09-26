package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.entity.Entity;
import com.prometis.appbuilder.app.entity.EntityColumn;
import com.prometis.appbuilder.app.entity.EntityRepository;
import com.prometis.appbuilder.app.entity.code.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("console.EntityRestController")
@RequestMapping("/console/api/entity")
public class EntityRestController {
    @Autowired
    private EntityRepository entityRepository;

    @GetMapping("")
    public ResponseEntity<?> getEntity(
            @RequestParam(name = "entityName", required = false) String entityName
    ) {
        List<Entity> results;
        if(entityName != null) {
            Optional<Entity> entity = entityRepository.findByEntityName(entityName);
            results = new ArrayList<>();
            entity.ifPresent(results::add);
        }
        else {
            results = entityRepository.findAll();
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEntity(@PathVariable("id") Long id) {
        Entity results = entityRepository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @Transactional
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody Map<String,Object> params) {
        Map<String,Object> pEntity = (Map<String, Object>) params.get("entity");
        List<Map<String,Object>> pEntityColumn = (List<Map<String,Object>>) params.get("entityColumns");

        Long id = (pEntity.get("id") == null || ((String) pEntity.get("id")).isEmpty())
                ? null
                : Long.valueOf((String) pEntity.get("id"));

        List<EntityColumn> entityColumns = new ArrayList<>();
        for(Map<String,Object> p : pEntityColumn) {
            entityColumns.add(
                    EntityColumn.builder()
                            .columnName((String) p.get("columnName"))
                            .displayName((String) p.get("displayName"))
                            .useKey((Boolean) p.get("useKey"))
                            .orderNum((p.get("orderNum") !=null) ? (Integer) p.get("orderNum") : null)
                            .columnType(ColumnType.valueOf((String) p.get("columnType")))
                            .selectWhereType((p.get("selectWhereType") != null) ? SelectWhereType.valueOf((String) p.get("selectWhereType")) : null)
                            .selectWhereCompareOperator((p.get("selectWhereCompareOperator") != null) ? (String) p.get("selectWhereCompareOperator") : null)
                            .selectOrderByNum((p.get("selectOrderByNum") != null) ? (Integer) p.get("selectOrderByNum") : null)
                            .selectOrderBySortOrder((p.get("selectOrderBySortOrder") !=null) ? SortOrder.valueOf((String) p.get("selectOrderBySortOrder")): null)
                            .insertAutoValueType((p.get("insertAutoValueType") != null) ? AutoValueType.valueOf((String) p.get("insertAutoValueType")) : null)
                            .insertAutoValue((p.get("insertAutoValue") != null) ? (String) p.get("insertAutoValue") : null)
                            .insertNullResolveType((p.get("insertNullResolveType") != null) ? NullResolveType.valueOf((String) p.get("insertNullResolveType")) : null)
                            .updateAutoValueType((p.get("updateAutoValueType") != null) ? AutoValueType.valueOf((String) p.get("updateAutoValueType")) : null)
                            .updateAutoValue((p.get("updateAutoValue") != null) ? (String) p.get("updateAutoValue") : null)
                            .updateNullResolveType((p.get("updateNullResolveType") != null) ? NullResolveType.valueOf((String) p.get("updateNullResolveType")) : null)
                            .deleteAutoValueType((p.get("deleteAutoValueType") != null) ? AutoValueType.valueOf((String) p.get("deleteAutoValueType")) : null)
                            .deleteAutoValue((p.get("deleteAutoValue") != null) ? (String) p.get("deleteAutoValue") : null)
                            .build());
        }

        Entity entity;
        if (id == null) {
            entity = Entity.builder()
                    .entityName((String) pEntity.get("entityName"))
                    .displayName((String) pEntity.get("displayName"))
                    .dataSourceName((String) pEntity.get("dataSourceName"))
                    .tableName((String) pEntity.get("tableName"))
                    .entityColumns(entityColumns)
                    .build();
        }
        else {
            entity = entityRepository.findById(id)
                    .orElseThrow(RuntimeException::new);

            entity.update(
                    (String) pEntity.get("entityName"),
                    (String) pEntity.get("displayName"),
                    (String) pEntity.get("dataSourceName"),
                    (String) pEntity.get("tableName"),
                    entityColumns
            );
        }

        Entity results = entityRepository.save(entity);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEntity(@PathVariable("id") Long id) {
        Entity entity = entityRepository.findById(id)
                .orElseThrow(RuntimeException::new);

        entityRepository.deleteById(entity.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
