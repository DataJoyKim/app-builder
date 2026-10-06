package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.security.company.Company;
import com.prometis.appbuilder.app.security.company.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("console.CompanyRestController")
@RequestMapping("/{applicationId}/console/api/company")
public class CompanyRestController {
    @Autowired
    private CompanyRepository repository;

    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId) {
        List<Company> results = repository.findOrderedByApplicationId(applicationId);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        Company results = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        String companyCode = text(params.get("companyCode"));
        String companyName = text(params.get("companyName"));

        if(companyCode == null || companyName == null) {
            return error("회사코드와 회사명을 입력해주세요.");
        }

        if(repository.findByApplicationIdAndCompanyCode(applicationId, companyCode).isPresent()) {
            return error("이미 존재하는 회사코드입니다. [" + companyCode + "]");
        }

        Integer orderNum;
        try {
            orderNum = orderNumOf(params.get("orderNum"));
        }
        catch (NumberFormatException e) {
            return error("순서는 숫자로 입력해주세요.");
        }

        Company createdData = Company.builder()
                .applicationId(applicationId)
                .companyCode(companyCode)
                .companyName(companyName)
                .orderNum(orderNum)
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        Company savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        String companyCode = text(params.get("companyCode"));
        String companyName = text(params.get("companyName"));

        if(companyCode == null || companyName == null) {
            return error("회사코드와 회사명을 입력해주세요.");
        }

        if(repository.findByApplicationIdAndCompanyCode(applicationId, companyCode).filter(other -> !other.getId().equals(id)).isPresent()) {
            return error("이미 존재하는 회사코드입니다. [" + companyCode + "]");
        }

        Integer orderNum;
        try {
            orderNum = orderNumOf(params.get("orderNum"));
        }
        catch (NumberFormatException e) {
            return error("순서는 숫자로 입력해주세요.");
        }

        savedData.update(companyCode, companyName, orderNum);

        return new ResponseEntity<>(repository.save(savedData), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        Company savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private static String text(Object value) {
        if(value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();

        return text.isEmpty() ? null : text;
    }

    // 비어 있으면 순서 없음 (맨 뒤). 화면에 따라 숫자 또는 문자열로 온다
    private static Integer orderNumOf(Object value) {
        String text = text(value);
        return text == null ? null : Integer.valueOf(text);
    }

    private static ResponseEntity<?> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
