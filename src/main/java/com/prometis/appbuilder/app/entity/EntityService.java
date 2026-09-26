package com.prometis.appbuilder.app.entity;

import com.prometis.appbuilder.app.entity.code.EntityResultCode;
import com.prometis.appbuilder.app.executor.sql.SqlExecutor;
import com.prometis.appbuilder.app.executor.sql.parameterbind.ParameterBindType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityService {
    private final EntityRepository entityRepository;
    private final EntityConfig entityConfig;

    public EntityResult execute(String entityName, EntityRequest params) {
        Entity entity = entityRepository.findByEntityName(entityName)
                                        .orElseThrow();

        List<EntitySqlQuery> entitySqlQueryList = entity.generateQuery(entityConfig, params);

        SqlExecutor sqlExecutor = SqlExecutor.createSqlExecutor(entity.getDataSourceName());

        List<Map<String,Object>> results = new ArrayList<>();

        int failed = 0;
        for(EntitySqlQuery entitySqlQuery : entitySqlQueryList) {
            try {
                List<Map<String, Object>> resultData = sqlExecutor.execute(entityName, entitySqlQuery.getSqlQuery(), ParameterBindType.NAME_BIND);

                for(Map<String, Object> result : resultData) {
                    result.put(entityConfig.getSeqParamKeyName(), entitySqlQuery.getSeq());
                }

                results.addAll(resultData);
            }
            catch (SQLException e) {
                log.error("error",e);
                Map<String,Object> resultData = new HashMap<>();
                resultData.put(entityConfig.getSeqParamKeyName(), entitySqlQuery.getSeq());
                resultData.put("errorCode", e.getErrorCode());
                resultData.put("message", e.getMessage());
                results.add(resultData);
                failed++;
            }
        }

        if(failed > 0) {
            return EntityResult.createEntityResult(EntityResultCode.FAILURE,results);
        }
        else {
            return EntityResult.createEntityResult(EntityResultCode.SUCCESS,results);
        }
    }
}
