package com.prometis.appbuilder.app.restapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 열거형 컬럼에 허용값 CHECK 제약이 생기면 ddl-auto update 가 그 제약을 늘려주지 않아서,
 * 열거형에 값을 추가하는 순간 기존 DB 에서 저장이 깨진다(예: ParameterIn.RESPONSE, FunctionType.ERROR_MESSAGE).
 * 그래서 열거형은 컨버터로 문자열 컬럼에 담고, 테이블을 새로 만들어도 CHECK 제약이 생기지 않아야 한다.
 */
@DataJpaTest
class EnumColumnConstraintTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void 열거형_컬럼에는_허용값_CHECK_제약이_없다() {
        List<Map<String, Object>> constraints = jdbcTemplate.queryForList("""
                SELECT tc.TABLE_NAME, cc.CHECK_CLAUSE
                  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                  JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc ON cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                 WHERE tc.CONSTRAINT_TYPE = 'CHECK'
                   AND tc.TABLE_NAME IN ('REST_API', 'REST_API_PARAMETER', 'WORKFLOW_NODE', 'FILE_HANDLER', 'DATA_SOURCE_FILE_STORAGE')
                """);

        assertEquals(List.of(), constraints);
    }
}
