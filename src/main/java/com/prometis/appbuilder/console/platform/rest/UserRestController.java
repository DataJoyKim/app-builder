package com.prometis.appbuilder.console.platform.rest;

import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserAuthority;
import com.prometis.appbuilder.platform.user.UserRepository;
import com.prometis.core.crypto.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController("console.UserRestController")
@RequestMapping("/console/api/user")
public class UserRestController {
    @Autowired
    private UserRepository repository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("")
    public ResponseEntity<?> getList() {
        List<User> results = repository.findAll();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        User results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> insert(@RequestBody Map<String,Object> params) {
        String loginId = trim(params.get("loginId"));
        String userName = trim(params.get("userName"));
        String email = trim(params.get("email"));
        String authority = (String) params.get("authority");
        String password = (String) params.get("password");
        String checkPassword = (String) params.get("checkPassword");

        String error = null;
        if(isBlank(loginId)) error = "로그인ID를 입력해주세요.";
        else if(isBlank(userName)) error = "성명을 입력해주세요.";
        else if(isBlank(email)) error = "email을 입력해주세요.";
        else if(!UserAuthority.contains(authority)) error = "권한을 선택해주세요.";
        else if(isBlank(password)) error = "비밀번호를 입력해주세요.";
        else if(!password.equals(checkPassword)) error = "비밀번호 확인이 일치하지 않습니다.";
        else if(repository.findByLoginId(loginId).isPresent()) error = "이미 존재하는 로그인ID입니다.";

        if(error != null) {
            return new ResponseEntity<>(Map.of("message", error), HttpStatus.BAD_REQUEST);
        }

        User saved = repository.save(User.builder()
                .loginId(loginId)
                .userName(userName)
                .email(email)
                .authority(authority)
                .password(passwordEncoder.encode(password))
                .build());

        return new ResponseEntity<>(Map.of("id", saved.getId()), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        User savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        String authority = (String) params.get("authority");
        if(!UserAuthority.contains(authority)) {
            return new ResponseEntity<>(Map.of("message", "권한을 선택해주세요."), HttpStatus.BAD_REQUEST);
        }

        savedData.update(
                (String) params.get("loginId"),
                (String) params.get("userName"),
                (String) params.get("email"),
                authority
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        User savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String trim(Object value) {
        return value == null ? null : ((String) value).trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
