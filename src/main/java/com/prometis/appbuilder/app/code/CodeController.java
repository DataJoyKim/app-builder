package com.prometis.appbuilder.app.code;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/{applicationId}/code")
public class CodeController {
    @Autowired
    CodeService codeService;


    @PostMapping("")
    public ResponseEntity<?> getCommonCode(
            @PathVariable("applicationId") String applicationId,
            @RequestBody List<CodeRequest> params
    ) {
        Map<String, List<CodeResponse>> response = codeService.getCode(applicationId, params);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
