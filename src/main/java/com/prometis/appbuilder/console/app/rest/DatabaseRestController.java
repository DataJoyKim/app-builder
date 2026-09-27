package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.datasource.ConnectValidation;
import com.prometis.appbuilder.app.datasource.LookupKey;
import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseRegister;
import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMeta;
import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseValidator;
import com.prometis.appbuilder.app.datasource.database.DatabaseKind;
import com.prometis.appbuilder.app.datasource.database.schema.ColumnSchema;
import com.prometis.appbuilder.app.datasource.database.schema.TableSchemaReader;
import com.prometis.appbuilder.console.app.repository.ConsoleDatabaseMetaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("console.DatabaseRestController")
@RequestMapping("/{applicationId}/console/api/datasource/database")
public class DatabaseRestController {
    @Autowired
    private ConsoleDatabaseMetaRepository databaseMetaRepository;
    @Autowired
    private DataSourceDatabaseValidator dataSourceDatabaseValidator;

    @GetMapping("")
    public ResponseEntity<?> getDataSource(@PathVariable("applicationId") String applicationId) {
        List<DataSourceDatabaseMeta> results = databaseMetaRepository.findByApplicationId(applicationId);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDataSource(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        DataSourceDatabaseMeta results = databaseMetaRepository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> createDatabase(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        databaseMetaRepository.insert(
                applicationId,
                (String) params.get("dataSourceName"),
                (String) params.get("displayName"),
                (String) params.get("note"),
                (String) params.get("url"),
                (String) params.get("username"),
                (String) params.get("password"),
                DatabaseKind.valueOf((String) params.get("databaseKind")),
                Integer.valueOf((String) params.get("maximumPoolSize")),
                Integer.valueOf((String) params.get("minimumIdle")),
                Integer.valueOf((String) params.get("connectionTimeout")),
                Integer.valueOf((String) params.get("validationTimeout"))
        );

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDatabase(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id,
            @RequestBody Map<String,Object> params
    ) {
        DataSourceDatabaseMeta databaseMeta = databaseMetaRepository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        databaseMetaRepository.update(
                databaseMeta.getId(),
                (String) params.get("dataSourceName"),
                (String) params.get("displayName"),
                (String) params.get("note"),
                (String) params.get("url"),
                (String) params.get("username"),
                (String) params.get("password"),
                DatabaseKind.valueOf((String) params.get("databaseKind")),
                Integer.valueOf((String) params.get("maximumPoolSize")),
                Integer.valueOf((String) params.get("minimumIdle")),
                Integer.valueOf((String) params.get("connectionTimeout")),
                Integer.valueOf((String) params.get("validationTimeout"))
        );

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDatabase(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id
    ) {
        DataSourceDatabaseMeta dataSource = databaseMetaRepository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        databaseMetaRepository.deleteById(dataSource.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<?> refreshDataSource(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id
    ) {
        DataSourceDatabaseMeta metadata = databaseMetaRepository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        DataSourceDatabaseRegister.registry(metadata);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }
    @GetMapping("/{id}/connect-valid")
    public ResponseEntity<?> validConnectDataSource(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id
    ) {
        DataSourceDatabaseMeta metadata = databaseMetaRepository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        ConnectValidation validate = dataSourceDatabaseValidator.validateConnect(metadata, DataSourceDatabaseRegister.getDataSourceMap());

        return new ResponseEntity<>(validate, HttpStatus.OK);
    }

    @GetMapping("/{dataSourceName}/table/{tableName}/columns")
    public ResponseEntity<?> getTableColumns(@PathVariable("applicationId") String applicationId, @PathVariable("dataSourceName") String dataSourceName,
            @PathVariable("tableName") String tableName
    ) {
        DataSourceDatabaseMeta metadata = databaseMetaRepository.findByApplicationIdAndDataSourceName(applicationId, dataSourceName)
                .orElseThrow(RuntimeException::new);

        DataSource dataSource = DataSourceDatabaseRegister.getDataSource(LookupKey.generateKey(applicationId, dataSourceName));
        if (dataSource == null) {
            throw new RuntimeException("초기화되지 않은 데이터소스입니다. [dataSourceName:" + dataSourceName + "]");
        }

        try {
            List<ColumnSchema> results = TableSchemaReader.readColumns(metadata.getDatabaseKind(), dataSource, tableName);

            return new ResponseEntity<>(results, HttpStatus.OK);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getDataSourceSummary(@PathVariable("applicationId") String applicationId) {
        Map<String, Object> results = new HashMap<>();

        List<DataSourceDatabaseMeta> metadata = databaseMetaRepository.findByApplicationId(applicationId);

        results.put("metadata",metadata);
        results.put("dataSources", DataSourceDatabaseRegister.getDataSourceMap());

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}
