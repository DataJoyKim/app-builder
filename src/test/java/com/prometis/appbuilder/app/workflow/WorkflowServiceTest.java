package com.prometis.appbuilder.app.workflow;

import com.prometis.appbuilder.app.restclient.RestClientBody;
import com.prometis.appbuilder.app.restclient.code.MessageDataType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class WorkflowServiceTest {
    @Test
    public void requestBody() {
        List<RestClientBody> bodys = new ArrayList<>();

        bodys.add(RestClientBody.builder().paramName("userId").parentParamName("ROOT").build());
        //bodys.add(RestClientBody.builder().parameterName("userNm").parentParameterName("ROOT").build());
        bodys.add(RestClientBody.builder().paramName("evalData").parentParamName("ROOT").dataType(MessageDataType.OBJECT).build());
        bodys.add(RestClientBody.builder().paramName("evalEmpId").parentParamName("evalData").build());
        //bodys.add(RestClientBody.builder().parameterName("score").parentParameterName("evalData").build());
        bodys.add(RestClientBody.builder().paramName("desc").parentParamName("evalData").build());
        bodys.add(RestClientBody.builder().paramName("goals").parentParamName("evalData").build());
        bodys.add(RestClientBody.builder().paramName("goalId").parentParamName("goals").dataType(MessageDataType.ARRAY).build());
        bodys.add(RestClientBody.builder().paramName("goalName").parentParamName("goals").build());
        bodys.add(RestClientBody.builder().paramName("goalKind").parentParamName("goals").build());
        // bodys.add(RestClientBody.builder().parameterName("org").parentParameterName("ROOT").dataType(MessageDataType.ARRAY).build());;
        // bodys.add(RestClientBody.builder().parameterName("orgCd").parentParameterName("org").build());
        // bodys.add(RestClientBody.builder().parameterName("orgNm").parentParameterName("org").build());

        Map<String,Object> params = new HashMap<>();

        params.put("userId",1551192);
        params.put("userNm","김낙영");
        params.put("userKind","EMP");

        Map<String, Object> evalData =new HashMap<>();
        evalData.put("evalEmpId","NM11705003");
        evalData.put("evalEmpNm","김낙영");
        evalData.put("score",1);
        evalData.put("desc","테스트입니다");
        List<Map<String,Object>> goals = new ArrayList<>();
        Map<String,Object> goal1 = new HashMap<>();
        goal1.put("goalId",1);
        goal1.put("goalName","목표1");
        goal1.put("goalKind","구분");
        goals.add(goal1);
        Map<String,Object> goal2 = new HashMap<>();
        goal2.put("goalId",2);
        goal2.put("goalName","목표2");
        goal2.put("goalKind","구분2");
        goals.add(goal2);

        evalData.put("goals",goals);
        params.put("evalData",evalData);

        List<Map<String,Object>> orgs = new ArrayList<>();
        Map<String,Object> org1 = new HashMap<>();
        org1.put("orgCd","ME0001");
        org1.put("orgNm","HR시스템팀");
        orgs.add(org1);
        Map<String,Object> org2 = new HashMap<>();
        org2.put("orgCd","OR0001");
        org2.put("orgNm","인사실");
        orgs.add(org2);

        params.put("org",orgs);

        Map<String, List<RestClientBody>> parent = bodys.stream().collect(Collectors.groupingBy(RestClientBody::getParentParamName));

        //Map<String,Object> requestBody = RestClient.createMessage("ROOT",params,parent);

//        System.out.println(requestBody.toString());
    }
}