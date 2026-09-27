package com.prometis.appbuilder.app.restapi;

import com.prometis.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * /{applicationId}/rest/** 요청 경로를 applicationId 와 API 경로로 나누는 규칙을 확인한다.
 */
class RestApiRequestPathTest {

    private static RestApiExecuteService.RequestPath parse(String uri) throws BusinessException {
        return RestApiExecuteService.requestPathOf(new MockHttpServletRequest("GET", uri));
    }

    @Test
    public void applicationId와_API_경로를_나눈다() throws Exception {
        assertEquals(new RestApiExecuteService.RequestPath("ehr", "/goals/42"), parse("/ehr/rest/goals/42"));
        assertEquals(new RestApiExecuteService.RequestPath("hr-portal", "/goals/{x}/items"), parse("/hr-portal/rest/goals/{x}/items"));
    }

    @Test
    public void 끝의_슬래시는_떼고_경로가_없으면_루트로_본다() throws Exception {
        assertEquals(new RestApiExecuteService.RequestPath("ehr", "/goals"), parse("/ehr/rest/goals/"));
        assertEquals(new RestApiExecuteService.RequestPath("ehr", "/"), parse("/ehr/rest"));
        assertEquals(new RestApiExecuteService.RequestPath("ehr", "/"), parse("/ehr/rest/"));
    }

    @Test
    public void 컨텍스트_경로는_먼저_뗀다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/ehr/rest/goals");
        request.setContextPath("/app");

        assertEquals(new RestApiExecuteService.RequestPath("ehr", "/goals"), RestApiExecuteService.requestPathOf(request));
    }

    // 예전 /rest/** 경로와, /{applicationId}/rest 모양이 아닌 경로는 정의된 API 가 있을 수 없으므로 404
    @ParameterizedTest
    @ValueSource(strings = {"/rest/goals", "/rest", "/ehr/restful/goals", "/ehr", "/ehr/", "//rest/goals", "/ehr/api/goals", "/"})
    public void 모양이_맞지_않으면_404(String uri) {
        BusinessException e = assertThrows(BusinessException.class, () -> parse(uri));

        assertEquals(RestApiErrorMessage.NOT_FOUND_API.getCode(), e.getCode());
    }
}
