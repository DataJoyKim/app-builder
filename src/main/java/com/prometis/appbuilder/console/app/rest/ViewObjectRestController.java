package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.view.ViewObjectRepository;
import com.prometis.appbuilder.app.view.ViewSourceException;
import com.prometis.appbuilder.app.view.ViewSourceStorage;
import com.prometis.appbuilder.app.view.code.ObjectType;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController("console.ViewObjectRestController")
@RequestMapping("/{applicationId}/console/api/object")
public class ViewObjectRestController {
    @Autowired
    private ViewObjectRepository repository;
    @Autowired
    private ViewSourceStorage viewSourceStorage;

    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId, @RequestParam(name = "objectCode", required = false) String objectCode
    ) {
        List<ViewObject> results;
        if(objectCode != null) {
            Optional<ViewObject> object = repository.findByApplicationIdAndObjectCode(applicationId, objectCode);
            results = new ArrayList<>();
            object.ifPresent(results::add);
        }
        else {
            results = repository.findByApplicationId(applicationId);
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        ViewObject results = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        String objectCode = trim(params.get("objectCode"));
        String objectName = trim(params.get("objectName"));
        ObjectType type = typeOf(params.get("type"));

        if(!ViewObject.isValidCode(objectCode)) {
            return error("오브젝트코드는 " + ViewObject.CODE_RULE + " 이어야 합니다.");
        }
        if(objectName.isEmpty()) {
            return error("오브젝트명을 입력해주세요.");
        }
        if(type == null) {
            return error("유형(화면빌더/파일)을 선택해주세요.");
        }
        if(repository.findByApplicationIdAndObjectCode(applicationId, objectCode).isPresent()) {
            return error("이미 있는 오브젝트코드입니다. [" + objectCode + "]");
        }

        ViewObject createdData = ViewObject.builder()
                .applicationId(applicationId)
                .objectCode(objectCode)
                .objectName(objectName)
                .type(type)
                .useAuthValidation(booleanOf(params.get("useAuthValidation"), true))
                .usePermissionValidation(booleanOf(params.get("usePermissionValidation"), true))
                .useToolbar(booleanOf(params.get("useToolbar"), true))
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    // 오브젝트코드와 유형은 만든 뒤에 바꾸지 않는다 (요청에 다른 값이 와도 무시한다)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        ViewObject savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        String objectName = trim(params.get("objectName"));
        if(objectName.isEmpty()) {
            return error("오브젝트명을 입력해주세요.");
        }

        savedData.update(
                objectName,
                booleanOf(params.get("useAuthValidation"), true),
                booleanOf(params.get("usePermissionValidation"), true),
                booleanOf(params.get("useToolbar"), true)
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        ViewObject savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        // FILE 화면은 디스크의 HTML 소스도 지운다
        if(ObjectType.FILE.equals(savedData.getType()) && ViewObject.isValidCode(savedData.getObjectCode())) {
            try {
                viewSourceStorage.delete(applicationId, savedData.getObjectCode());
            }
            catch (ViewSourceException e) {
                return error("오브젝트는 삭제했지만 " + e.getMessage());
            }
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private static String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static ObjectType typeOf(Object value) {
        String text = trim(value);
        return Arrays.stream(ObjectType.values())
                .filter(type -> type.name().equals(text))
                .findFirst()
                .orElse(null);
    }

    private static Boolean booleanOf(Object value, boolean defaultValue) {
        String text = trim(value);
        return text.isEmpty() ? defaultValue : Boolean.valueOf(text);
    }

    private static ResponseEntity<?> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
