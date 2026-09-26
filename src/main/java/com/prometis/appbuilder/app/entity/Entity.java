package com.prometis.appbuilder.app.entity;

import com.prometis.appbuilder.app.entity.code.EntityStatus;
import com.prometis.appbuilder.app.entity.query.EntityQueryGenerator;
import com.prometis.appbuilder.app.entity.query.EntityQueryGeneratorFactory;
import com.prometis.appbuilder.app.entity.query.FailedQueryGenerationException;
import com.prometis.appbuilder.app.executor.sql.SqlParameter;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="ENTITY_UQ",columnNames={"entityName"})})
@jakarta.persistence.Entity
public class Entity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String entityName;

    @Column(nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false, length = 100)
    private String dataSourceName;

    @Column(nullable = false, length = 100)
    private String tableName;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval=true)
    @JoinColumn(name = "ENTITY_ID")
    @OrderBy("orderNum ASC")
    private List<EntityColumn> entityColumns;

    public List<EntitySqlQuery> generateQuery(EntityConfig config, EntityRequest params) {
        List<Map<String, Object>> contents = params.getContents();

        List<EntitySqlQuery> entitySqlQueryList = new ArrayList<>();

        for(Map<String, Object> content : contents) {
            String seq = String.valueOf(content.get(config.getSeqParamKeyName()));
            EntityStatus status = EntityStatus.valueOf((String) content.get(config.getStatusParamKeyName()));

            String sql = createSql(status);

            List<SqlParameter> sqlParameters = createSqlParameters(content);

            entitySqlQueryList.add(EntitySqlQuery.createEntitySqlQuery(seq, sql, sqlParameters));
        }

        return entitySqlQueryList;
    }

    private String createSql(EntityStatus status) {
        EntityQueryGenerator entityQueryGenerator = EntityQueryGeneratorFactory.instance(status);

        try {
            return entityQueryGenerator.generate(this.tableName, this.entityColumns);
        }
        catch (FailedQueryGenerationException e) {
            throw new RuntimeException(e);
        }
    }

    private List<SqlParameter> createSqlParameters(Map<String, Object> content) {
        List<SqlParameter> sqlParameters = new ArrayList<>();
        int i=0;
        for(EntityColumn column : this.entityColumns) {

            sqlParameters.add(SqlParameter.createSqlParameter(
                    column.getColumnName(),
                    i,
                    content.get(column.getColumnName())
            ));
            i++;
        }
        return sqlParameters;
    }

    public void update(
            String entityName,
            String displayName,
            String dataSourceName,
            String tableName,
            List<EntityColumn> entityColumns
    ) {
        this.entityName = entityName;
        this.displayName = displayName;
        this.dataSourceName = dataSourceName;
        this.tableName = tableName;

        this.entityColumns.clear();
        this.entityColumns.addAll(entityColumns);
    }
}
