package com.prometis.appbuilder.app.query;

import com.prometis.appbuilder.app.entity.EntityColumn;
import com.prometis.appbuilder.app.entity.code.ColumnType;
import com.prometis.appbuilder.app.entity.code.NullResolveType;
import com.prometis.appbuilder.app.entity.code.SelectWhereType;
import com.prometis.appbuilder.app.entity.code.SortOrder;
import com.prometis.appbuilder.app.entity.query.EntityQueryGenerator;
import com.prometis.appbuilder.app.entity.query.InsertQuery;
import com.prometis.appbuilder.app.entity.query.SelectQuery;
import com.prometis.appbuilder.app.entity.query.UpdateQuery;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class EntityQueryGeneratorTest {

    @Test
    void generateInsertQuery() {
        // Given
        String tableName = "goal";

        List<EntityColumn> entityColumns = new ArrayList<>();
        entityColumns.add(EntityColumn.builder()
                .columnName("goalId")
                .value(null)
                .columnType(ColumnType.NUMBER)
                .insertNullResolveType(NullResolveType.EXCEPTION)
                .build());

        entityColumns.add(EntityColumn.builder()
                .columnName("goalName")
                .value("테스트")
                .columnType(ColumnType.STRING)
                .insertNullResolveType(NullResolveType.SET_NULL)
                .build());

        entityColumns.add(EntityColumn.builder()
                .columnName("goalKind")
                .value("EVAL")
                .columnType(ColumnType.STRING)
                .insertNullResolveType(NullResolveType.SET_NULL)
                .build());

        // When
        EntityQueryGenerator generator = new InsertQuery();

        String query = generator.generate(tableName, entityColumns);

        // Then
        System.out.println(query);
    }

    @Test
    void generateUpdateQuery() {
        // Given
        String tableName = "goal";

        List<EntityColumn> entityColumns = new ArrayList<>();
        entityColumns.add(EntityColumn.builder()
                .useKey(true)
                .columnName("goalId")
                .value(1L)
                .columnType(ColumnType.NUMBER)
                .build());

        entityColumns.add(EntityColumn.builder()
                .useKey(false)
                .columnName("goalName")
                .value("테스트")
                .columnType(ColumnType.STRING)
                .build());

        entityColumns.add(EntityColumn.builder()
                .useKey(false)
                .columnName("goalKind")
                .value("EVAL")
                .columnType(ColumnType.STRING)
                .build());

        // When
        EntityQueryGenerator generator = new UpdateQuery();

        String query = generator.generate(tableName, entityColumns);

        // Then
        System.out.println(query);
    }

    @Test
    void generateSelectQuery() {
        // Given
        String tableName = "goal";

        List<EntityColumn> entityColumns = new ArrayList<>();
        entityColumns.add(EntityColumn.builder()
                .useKey(true)
                .columnName("goalId")
                .value(1L)
                .columnType(ColumnType.NUMBER)
                .selectWhereCompareOperator("=")
                .selectWhereType(SelectWhereType.COMPARE_REQUIRED)
                .build());

        entityColumns.add(EntityColumn.builder()
                .useKey(false)
                .columnName("goalName")
                .value("테스트")
                .columnType(ColumnType.STRING)
                .selectWhereCompareOperator("like")
                .selectWhereType(SelectWhereType.COMPARE_NOT_REQUIRED)
                .build());

        entityColumns.add(EntityColumn.builder()
                .useKey(false)
                .columnName("goalKind")
                .value("EVAL")
                .columnType(ColumnType.STRING)
                .selectWhereCompareOperator("=")
                .selectWhereType(SelectWhereType.COMPARE_NOT_REQUIRED)
                .selectOrderByNum(1)
                .selectOrderBySortOrder(SortOrder.DESC)
                .build());

        entityColumns.add(EntityColumn.builder()
                .useKey(false)
                .columnName("weight")
                .value(100)
                .columnType(ColumnType.NUMBER)
                .selectWhereCompareOperator(">")
                .selectWhereType(SelectWhereType.COMPARE_NOT_REQUIRED)
                .selectOrderByNum(2)
                .selectOrderBySortOrder(SortOrder.ASC)
                .build());

        // When
        EntityQueryGenerator generator = new SelectQuery();

        String query = generator.generate(tableName, entityColumns);

        // Then
        System.out.println(query);
    }

}