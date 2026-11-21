package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.view.ViewObjectRepository;
import com.prometis.appbuilder.app.view.ViewSourceException;
import com.prometis.appbuilder.app.view.ViewSourceStorage;
import com.prometis.appbuilder.app.view.code.ObjectType;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * FILE 유형 화면의 HTML 소스. 소스는 DB 가 아니라 {platform.view.root-path}/{applicationId}/views/{objectCode}.html 에 저장된다 (ViewSourceStorage).
 * 화면빌더 콘솔에서 편집기 내용을 저장(PUT)하거나 .html 파일을 올린다(POST /upload).
 */
@RestController("console.ViewObjectSourceRestController")
@RequestMapping("/{applicationId}/console/api/object-source")
@RequiredArgsConstructor
public class ViewObjectSourceRestController {
    private final ViewObjectRepository viewObjectRepository;
    private final ViewSourceStorage viewSourceStorage;

    @GetMapping("")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @RequestParam("objectCode") String objectCode) {
        ResponseEntity<?> invalid = checkFileObject(applicationId, objectCode);
        if(invalid != null) {
            return invalid;
        }

        try {
            Optional<String> content = viewSourceStorage.read(applicationId, objectCode);
            return new ResponseEntity<>(result(applicationId, objectCode, content.isPresent(), content.orElse("")), HttpStatus.OK);
        }
        catch (ViewSourceException e) {
            return error(e.getMessage());
        }
    }

    @PutMapping("")
    public ResponseEntity<?> save(@PathVariable("applicationId") String applicationId, @RequestBody Map<String, Object> params) {
        String objectCode = params.get("objectCode") == null ? null : String.valueOf(params.get("objectCode"));
        ResponseEntity<?> invalid = checkFileObject(applicationId, objectCode);
        if(invalid != null) {
            return invalid;
        }

        Object content = params.get("content");
        try {
            viewSourceStorage.write(applicationId, objectCode, content == null ? "" : String.valueOf(content));
            return new ResponseEntity<>(result(applicationId, objectCode, true, null), HttpStatus.OK);
        }
        catch (ViewSourceException e) {
            return error(e.getMessage());
        }
    }

    // .html 파일을 올리면 그 내용으로 소스를 바꾸고, 저장한 내용을 돌려준다 (편집기에 다시 채운다)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@PathVariable("applicationId") String applicationId,
                                    @RequestParam("objectCode") String objectCode,
                                    @RequestPart("file") MultipartFile file) {
        ResponseEntity<?> invalid = checkFileObject(applicationId, objectCode);
        if(invalid != null) {
            return invalid;
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if(!filename.endsWith(".html") && !filename.endsWith(".htm")) {
            return error(".html 또는 .htm 파일만 올릴 수 있습니다.");
        }
        if(file.getSize() > viewSourceStorage.maxFileSize()) {
            return error("화면 소스는 " + (viewSourceStorage.maxFileSize() / 1024) + "KB 까지 저장할 수 있습니다.");
        }

        try {
            String content = viewSourceStorage.writeBytes(applicationId, objectCode, file.getBytes());
            return new ResponseEntity<>(result(applicationId, objectCode, true, content), HttpStatus.OK);
        }
        catch (ViewSourceException e) {
            return error(e.getMessage());
        }
        catch (IOException e) {
            return error("올린 파일을 읽지 못했습니다.");
        }
    }

    // 이 애플리케이션의 FILE 유형 오브젝트인지. 아니면 오류 응답, 맞으면 null
    private ResponseEntity<?> checkFileObject(String applicationId, String objectCode) {
        Optional<ViewObject> viewObject = objectCode == null
                ? Optional.empty()
                : viewObjectRepository.findByApplicationIdAndObjectCode(applicationId, objectCode);
        if(viewObject.isEmpty()) {
            Map<String, Object> body = new HashMap<>();
            body.put("message", "오브젝트가 없습니다. [" + objectCode + "]");
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
        if(!ObjectType.FILE.equals(viewObject.get().getType())) {
            return error("파일 유형 오브젝트만 HTML 소스를 가질 수 있습니다. [" + objectCode + "]");
        }
        return null;
    }

    private static Map<String, Object> result(String applicationId, String objectCode, boolean exists, String content) {
        Map<String, Object> result = new HashMap<>();
        result.put("objectCode", objectCode);
        result.put("exists", exists);
        result.put("path", ViewSourceStorage.displayPath(applicationId, objectCode));
        if(content != null) {
            result.put("content", content);
        }
        return result;
    }

    private static ResponseEntity<?> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
