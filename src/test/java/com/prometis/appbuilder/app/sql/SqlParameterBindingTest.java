package com.prometis.appbuilder.app.sql;

import com.prometis.appbuilder.app.executor.sql.SqlParameter;
import com.prometis.appbuilder.app.executor.sql.SqlQuery;
import com.prometis.appbuilder.app.executor.sql.parameterbind.NameBind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class SqlParameterBindingTest {

    @Test
    public void nameBindingSqlParameterEmptyTest() {
        //given
        String sql = "select * from emp where company_cd = #{company_cd} and emp_id = #{emp_id} and org_id= #{org_id} and appr_emp_id = #{emp_id} order by emp_id";

        List<SqlParameter> sqlParameters = new ArrayList<>();

        // when
        SqlQuery sqlQuery = SqlQuery.createSqlQuery(sql, sqlParameters);

        NameBind bind = new NameBind();

        SqlQuery convertedSqlQuery = bind.binding(sqlQuery);

        // then
        Assertions.assertFalse(convertedSqlQuery.getSql().contains("#{company_cd}"));
        Assertions.assertFalse(convertedSqlQuery.getSql().contains("#{emp_id}"));
        Assertions.assertFalse(convertedSqlQuery.getSql().contains("#{org_id}"));

        Assertions.assertEquals(0, convertedSqlQuery.getSqlParameters().size());
    }

    @Test
    public void nameBindingTest() {
        //given
        String sql = "select * from emp where company_cd = #{company_cd} and emp_id = #{emp_id} and org_id= #{org_id} and appr_emp_id = #{emp_id} order by emp_id";

        List<SqlParameter> sqlParameters = new ArrayList<>();
        sqlParameters.add(SqlParameter.createSqlParameter("emp_id", 1,1155991));
        sqlParameters.add(SqlParameter.createSqlParameter("company_cd", 2,"01"));
        sqlParameters.add(SqlParameter.createSqlParameter("org_id", 3, 12422));
        sqlParameters.add(SqlParameter.createSqlParameter("emp_id", 4,1155991));

        // when
        SqlQuery sqlQuery = SqlQuery.createSqlQuery(sql, sqlParameters);

        NameBind bind = new NameBind();

        SqlQuery convertedSqlQuery = bind.binding(sqlQuery);

        // then
        Assertions.assertFalse(convertedSqlQuery.getSql().contains("#{company_cd}"));
        Assertions.assertFalse(convertedSqlQuery.getSql().contains("#{emp_id}"));
        Assertions.assertFalse(convertedSqlQuery.getSql().contains("#{org_id}"));

        Assertions.assertEquals(4, convertedSqlQuery.getSqlParameters().size());

        for(SqlParameter p : convertedSqlQuery.getSqlParameters()) {
            if(p.getParameterIndex() == 1) {
                Assertions.assertEquals(p.getParameterName(), "company_cd");
            }
            else if(p.getParameterIndex() == 2) {
                Assertions.assertEquals(p.getParameterName(), "emp_id");
            }
            else if(p.getParameterIndex() == 3) {
                Assertions.assertEquals(p.getParameterName(), "org_id");
            }
            else if(p.getParameterIndex() == 4) {
                Assertions.assertEquals(p.getParameterName(), "emp_id");
            }
        }
    }

    @Test
    public void nameBindingDuplicationTest() {
        //given
        String sql = "select * from emp where company_cd = #{companyCd} and emp_id = #{empId} and org_id= #{orgId} and appr_emp_id = #{empid} order by emp_id";

        List<SqlParameter> sqlParameters = new ArrayList<>();
        sqlParameters.add(SqlParameter.createSqlParameter("companyCd", 1,"01"));
        sqlParameters.add(SqlParameter.createSqlParameter("empId", 2,1155991));
        sqlParameters.add(SqlParameter.createSqlParameter("orgId", 3, 12321));
        sqlParameters.add(SqlParameter.createSqlParameter("empid", 4, 1155991));

        // when
        SqlQuery sqlQuery = SqlQuery.createSqlQuery(sql, sqlParameters);

        NameBind bind = new NameBind();

        SqlQuery convertedSqlQuery = bind.binding(sqlQuery);

        // then
        Assertions.assertFalse(convertedSqlQuery.getSql().contains("#{companyCd}"));
        Assertions.assertFalse(convertedSqlQuery.getSql().contains("#{empId}"));
        Assertions.assertFalse(convertedSqlQuery.getSql().contains("#{orgId}"));
        Assertions.assertFalse(convertedSqlQuery.getSql().contains("#{empid}"));

        Assertions.assertEquals(4, convertedSqlQuery.getSqlParameters().size());

        for(SqlParameter p : convertedSqlQuery.getSqlParameters()) {
            if(p.getParameterIndex() == 1) {
                Assertions.assertEquals(p.getParameterName(), "companyCd");
            }
            else if(p.getParameterIndex() == 2) {
                Assertions.assertEquals(p.getParameterName(), "empId");
            }
            else if(p.getParameterIndex() == 3) {
                Assertions.assertEquals(p.getParameterName(), "orgId");
            }
            else if(p.getParameterIndex() == 4) {
                Assertions.assertEquals(p.getParameterName(), "empid");
            }
        }
    }
}