package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.image.ImageException;
import com.prometis.appbuilder.app.image.ImageProperties;
import com.prometis.appbuilder.app.image.ImageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("console.ImageRestController")
@RequestMapping("/{applicationId}/console/api/image")
public class ImageRestController {
    @Autowired
    private ImageService imageService;

    // 화면에서 업로드 전에 미리 걸러내도록 업로드 제한(application.yml platform.image)을 알려준다
    @GetMapping("/policy")
    public ResponseEntity<?> policy() {
        ImageProperties policy = imageService.policy();

        Map<String, Object> body = new HashMap<>();
        body.put("allowedExtensions", policy.allowedExtensions());
        body.put("maxFileSize", policy.maxFileSize().toBytes());

        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @GetMapping("/storage-type")
    public ResponseEntity<?> getStorageTypes(@PathVariable("applicationId") String applicationId) {
        return new ResponseEntity<>(imageService.getStorageTypes(applicationId), HttpStatus.OK);
    }

    @PostMapping("/storage-type")
    public ResponseEntity<?> createStorageType(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        try {
            return new ResponseEntity<>(imageService.createStorageType(applicationId, text(params.get("storageType")), text(params.get("displayName"))), HttpStatus.OK);
        }
        catch (ImageException e) {
            return error(e.getMessage());
        }
    }

    @PutMapping("/storage-type/{id}")
    public ResponseEntity<?> updateStorageType(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        try {
            return new ResponseEntity<>(imageService.updateStorageType(applicationId, id, text(params.get("displayName"))), HttpStatus.OK);
        }
        catch (ImageException e) {
            return error(e.getMessage());
        }
    }

    @DeleteMapping("/storage-type/{id}")
    public ResponseEntity<?> deleteStorageType(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        try {
            imageService.deleteStorageType(applicationId, id);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        catch (ImageException e) {
            return error(e.getMessage());
        }
    }

    @GetMapping("")
    public ResponseEntity<?> getImages(@PathVariable("applicationId") String applicationId, @RequestParam("storageType") String storageType) {
        return new ResponseEntity<>(imageService.getImages(applicationId, storageType), HttpStatus.OK);
    }

    // multipart/form-data: storageType, overwrite 필드 + 이미지 파일마다 files 파트와 filenames 필드 (같은 순서, 빈 값이면 원본 파일명에서 확장자를 뺀 이름)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            HttpServletRequest request,
            @PathVariable("applicationId") String applicationId,
            @RequestParam("storageType") String storageType,
            @RequestParam(name = "overwrite", required = false, defaultValue = "false") boolean overwrite,
            @RequestParam(name = "files", required = false) List<MultipartFile> files
    ) {
        // @RequestParam List<String> 은 값이 하나면 쉼표로 나눠버리므로 그대로 읽는다
        String[] filenames = request.getParameterValues("filenames");

        try {
            return new ResponseEntity<>(imageService.upload(applicationId, storageType, files,
                    filenames == null ? List.of() : Arrays.asList(filenames), overwrite), HttpStatus.OK);
        }
        catch (ImageException e) {
            return error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        try {
            imageService.deleteImage(applicationId, id);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        catch (ImageException e) {
            return error(e.getMessage());
        }
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static ResponseEntity<?> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
