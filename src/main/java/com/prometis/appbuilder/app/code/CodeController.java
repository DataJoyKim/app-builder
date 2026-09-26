package com.prometis.appbuilder.app.code;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/code")
public class CodeController {
    @Autowired
    CodeService codeService;


    @PostMapping("")
    public ResponseEntity<?> getCommonCode(@RequestBody List<CodeRequest> params) {
        Map<String, List<CodeResponse>> response = codeService.getCode(params);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
