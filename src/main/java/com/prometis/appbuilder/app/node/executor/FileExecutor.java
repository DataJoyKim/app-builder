package com.prometis.appbuilder.app.node.executor;

import com.prometis.appbuilder.app.dto.RequestMessage;
import com.prometis.appbuilder.app.file.FileRequest;
import com.prometis.appbuilder.app.file.FileResult;
import com.prometis.appbuilder.app.file.FileService;
import com.prometis.appbuilder.app.file.code.FileResultCode;
import com.prometis.appbuilder.app.node.NodeConfig;
import com.prometis.appbuilder.app.node.NodeExecutor;
import com.prometis.appbuilder.app.node.NodeResult;
import com.prometis.appbuilder.app.node.code.ResultType;
import com.prometis.appbuilder.app.security.domain.AuthenticatedUser;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FileExecutor implements NodeExecutor {
    private final FileService fileService;
    private final NodeConfig config;

    @Override
    public NodeResult execute(AuthenticatedUser user, String functionName, RequestMessage.Header header, List<Map<String, Object>> params) {

        ResultType resultType = ResultType.SUCCESS;
        List<Map<String, Object>> results = new ArrayList<>();

        if(header.getFiles() != null && !header.getFiles().isEmpty()) {
            // 업로드 파일과 요청메시지 행은 순서대로 짝짓는다. 행이 하나면 모든 파일에 같은 값을 쓰고,
            // 행이 여러 개인데 파일 수와 다르면 어느 파일에 어느 행을 써야할지 알 수 없으므로 실행하지 않는다.
            if(params.size() > 1 && params.size() != header.getFiles().size()) {
                return NodeResult.builder()
                        .resultType(ResultType.FAILURE)
                        .results(List.of(createResponseObj(
                                FileResult.failure("업로드한 파일 수와 요청메시지 행 수가 맞지 않습니다. [파일:"
                                        + header.getFiles().size() + ", 행:" + params.size() + "]"), null)))
                        .build();
            }

            for(int index=0; index < header.getFiles().size(); index++) {
                Map<String, Object> param = paramOf(params, index);
                MultipartFile multipartFile = header.getFiles().get(index);

                String seq = (String) param.get(config.getRequestMessageSeqKey());

                FileRequest fileRequest = FileRequest.builder()
                        .params(param)
                        .multipartFile(multipartFile)
                        .build();

                FileResult result = fileService.execute(functionName, fileRequest);

                if(result.getResultCode() == FileResultCode.FAILURE) {
                    resultType = ResultType.FAILURE;
                }

                Map<String, Object> responseObj = createResponseObj(result, seq);

                results.add(responseObj);
            }
        }
        else {
            for(Map<String, Object> param : params) {
                String seq = (String) param.get(config.getRequestMessageSeqKey());

                FileRequest fileRequest = FileRequest.builder()
                        .params(param)
                        .build();

                FileResult result = fileService.execute(functionName, fileRequest);

                if(result.getResultCode() == FileResultCode.FAILURE) {
                    resultType = ResultType.FAILURE;
                }

                Map<String, Object> responseObj = createResponseObj(result, seq);

                results.add(responseObj);
            }
        }

        return NodeResult.builder()
                .resultType(resultType)
                .results(results)
                .build();
    }

    // 요청메시지 행이 없으면 빈 파라미터로, 한 행이면 모든 파일에 그 행으로, 그 외에는 파일 순서에 맞는 행으로 실행한다.
    private static Map<String, Object> paramOf(List<Map<String, Object>> params, int index) {
        if(params.isEmpty()) {
            return Map.of();
        }

        return (params.size() == 1) ? params.get(0) : params.get(index);
    }

    @Nonnull
    private Map<String, Object> createResponseObj(FileResult result, String seq) {
        Map<String, Object> responseObj = new HashMap<>();
        if(result.getContent() != null) {
            responseObj.putAll(result.getContent());
        }
        responseObj.put(config.getRequestMessageSeqKey(), seq);
        responseObj.put("message", result.getMessage());
        responseObj.put("resultCode", result.getResultCode());
        return responseObj;
    }
}
