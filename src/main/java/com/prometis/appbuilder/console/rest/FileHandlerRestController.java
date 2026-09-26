package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.file.*;
import com.prometis.appbuilder.app.file.code.FileActionType;
import com.prometis.appbuilder.app.restapi.code.FileContentEncoding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.*;

@RestController("console.FileHandlerRestController")
@RequestMapping("/console/api/file")
public class FileHandlerRestController {
    @Autowired
    private FileHandlerRepository repository;
    @Autowired
    private FileService fileService;

    @GetMapping("")
    public ResponseEntity<?> getFileHandler(
            @RequestParam(name = "handlerName", required = false) String handlerName
    ) {
        List<FileHandler> results;
        if(handlerName != null) {
            Optional<FileHandler> fileHandler = repository.findByHandlerName(handlerName);
            results = new ArrayList<>();
            fileHandler.ifPresent(results::add);
        }
        else {
            results = repository.findAll();
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFileHandler(@PathVariable("id") Long id) {
        FileHandler results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @Transactional
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody Map<String,Object> params) {
        Long id = (params.get("id") == null || String.valueOf(params.get("id")).isEmpty())
                ? null
                : Long.valueOf(String.valueOf(params.get("id")));

        FileActionType actionType = FileActionType.valueOf((String) params.get("actionType"));
        FileContentEncoding fileContentEncoding = emptyToNull((String) params.get("fileContentEncoding")) == null
                ? null
                : FileContentEncoding.valueOf((String) params.get("fileContentEncoding"));

        FileHandler fileHandler;
        if (id == null) {
            fileHandler = FileHandler.builder()
                    .handlerName((String) params.get("handlerName"))
                    .displayName((String) params.get("displayName"))
                    .dataSourceName((String) params.get("dataSourceName"))
                    .actionType(actionType)
                    .filePath((String) params.get("filePath"))
                    .fileContentKey(emptyToNull((String) params.get("fileContentKey")))
                    .fileContentEncoding(fileContentEncoding)
                    .overwrite((Boolean) params.get("overwrite"))
                    .build();
        }
        else {
            fileHandler = repository.findById(id)
                    .orElseThrow(RuntimeException::new);

            fileHandler.update(
                    (String) params.get("handlerName"),
                    (String) params.get("displayName"),
                    (String) params.get("dataSourceName"),
                    actionType,
                    (String) params.get("filePath"),
                    emptyToNull((String) params.get("fileContentKey")),
                    fileContentEncoding,
                    (Boolean) params.get("overwrite")
            );
        }

        FileHandler results = repository.save(fileHandler);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFileHandler(@PathVariable("id") Long id) {
        FileHandler fileHandler = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(fileHandler.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 업로드 파일내용(Base64)이 커질 수 있어 본문으로 받는다.
    @PostMapping(value = "/{handlerName}/execute", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> execute(
            @PathVariable("handlerName") String handlerName,
            @RequestBody Map<String, Object> params
    ) {
        return executeHandler(handlerName, params == null ? Map.of() : params);
    }

    // MULTIPART 인코딩 정의용. 폼 필드는 파라미터로, 업로드된 파일은 워크플로우와 똑같이 업로드 파일로 넘긴다.
    @PostMapping(value = "/{handlerName}/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> executeMultipart(
            @PathVariable("handlerName") String handlerName,
            MultipartHttpServletRequest request
    ) {
        Map<String, Object> params = new HashMap<>();

        request.getParameterMap().forEach((key, values) -> {
            if(values == null || values.length == 0) {
                params.put(key, null);
            }
            else {
                params.put(key, (values.length == 1) ? values[0] : List.of(values));
            }
        });

        // 파일이 여러 개 올라와도 첫 번째 파일만 쓴다. (파일 노드는 한 번에 파일 하나를 다룬다)
        MultipartFile multipartFile = request.getFileMap().values().stream()
                .findFirst()
                .orElse(null);

        return executeHandler(handlerName, params, multipartFile);
    }

    private ResponseEntity<?> executeHandler(String handlerName, Map<String, Object> params) {
        return executeHandler(handlerName, params, null);
    }

    private ResponseEntity<?> executeHandler(String handlerName, Map<String, Object> params, MultipartFile multipartFile) {
        FileRequest request = FileRequest.builder()
                .params(params)
                .multipartFile(multipartFile)
                .build();

        FileResult results = fileService.execute(handlerName, request);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    private static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
