package com.prometis.appbuilder.app.code;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter @Setter @AllArgsConstructor @Builder
public class CodeResponse {
    private String code;
    private String name;

    public static List<CodeResponse> ofCommonCode(List<CommonCode> commonCodeList) {
        if(commonCodeList == null) {
            return new ArrayList<>();
        }

        List<CodeResponse> codeResponses = new ArrayList<>();
        for(CommonCode c : commonCodeList) {
            codeResponses.add(CodeResponse.builder()
                    .code(c.getCode())
                    .name(c.getName())
                    .build());
        }

        return codeResponses;
    }

    public static List<CodeResponse> ofSql(List<Map<String, Object>> results) {
        if(results == null) {
            return new ArrayList<>();
        }

        List<CodeResponse> codeResponses = new ArrayList<>();
        for(Map<String, Object> c : results) {
            codeResponses.add(CodeResponse.builder()
                    .code((String) c.get("code"))
                    .name((String) c.get("name"))
                    .build());
        }

        return codeResponses;
    }
}
