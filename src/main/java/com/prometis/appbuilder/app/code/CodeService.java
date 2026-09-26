package com.prometis.appbuilder.app.code;

import com.prometis.appbuilder.app.query.QueryRequest;
import com.prometis.appbuilder.app.query.QueryResult;
import com.prometis.appbuilder.app.query.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodeService {
    private final CommonCodeRepository commonCodeRepository;

    private final QueryService queryService;

    public Map<String, List<CodeResponse>> getCode(List<CodeRequest> params) {
        List<String> sqlCodes = new ArrayList<>();
        List<String> commonCodeKindCodes = new ArrayList<>();

        for(CodeRequest codeRequest : params) {
            if(codeRequest.getType() == CodeType.COMMON_CODE) {
                commonCodeKindCodes.add(codeRequest.getName());
            }
            else if(codeRequest.getType() == CodeType.SQL) {
                sqlCodes.add(codeRequest.getName());
            }
        }

        Map<String, List<CodeResponse>> response = createCommonCode(commonCodeKindCodes);

        response.putAll(createSqlCode(sqlCodes));

        return response;
    }

    private Map<String, List<CodeResponse>> createCommonCode(List<String> commonCodeKindCodes) {
        List<CommonCode> commonCodes = commonCodeRepository.findByCodeKindCodes(commonCodeKindCodes);

        Map<String, List<CommonCode>> commonCodeMap = commonCodes.stream().collect(Collectors.groupingBy(CommonCode::getCommonCodeKindCode));

        Map<String, List<CodeResponse>> response = new HashMap<>();

        for(String name : commonCodeKindCodes) {
            List<CommonCode> commonCodeList = commonCodeMap.get(name);

            response.put(name, CodeResponse.ofCommonCode(commonCodeList));
        }

        return response;
    }

    private Map<String, List<CodeResponse>> createSqlCode(List<String> sqlCodes) {
        Map<String, List<CodeResponse>> response = new HashMap<>();

        for(String name : sqlCodes) {
            QueryRequest queryParam = QueryRequest.builder()
                    .build();

            QueryResult queryResult = queryService.execute(name, queryParam);

            response.put(name, CodeResponse.ofSql(queryResult.getResults()));
        }

        return response;
    }
}
