package com.prometis.appbuilder.console.platform.rest;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserService;
import com.prometis.appbuilder.console.platform.dto.ApplicationAdminRequest;
import com.prometis.appbuilder.console.platform.dto.ApplicationAdminResponse;
import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 플랫폼 콘솔에서 애플리케이션별 관리자(AppUser.authority = APPLICATION_ADMIN)를 지정한다.
 * {id} 는 Application 의 DB id 다. (화면의 다른 애플리케이션 API 와 같다)
 */
@RestController
@RequestMapping("/console/api/application/{id}/admin")
public class ApplicationAdminRestController {
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AppUserService appUserService;

    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("id") Long id) {
        Optional<Application> application = applicationRepository.findById(id);
        if(application.isEmpty()) {
            return notFound("애플리케이션이 존재하지 않습니다.");
        }

        List<AppUser> admins = appUserService.getApplicationAdmins(application.get().getApplicationId());

        Map<Long, User> users = userRepository.findAllById(admins.stream().map(AppUser::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<ApplicationAdminResponse> results = admins.stream()
                .map(admin -> ApplicationAdminResponse.of(admin, users.get(admin.getUserId())))
                .toList();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    // 이미 애플리케이션 사용자면 권한만 APPLICATION_ADMIN 으로 바꾼다
    @PostMapping("")
    public ResponseEntity<?> grant(@PathVariable("id") Long id, @RequestBody ApplicationAdminRequest request) {
        Optional<Application> application = applicationRepository.findById(id);
        if(application.isEmpty()) {
            return notFound("애플리케이션이 존재하지 않습니다.");
        }

        if(request.getUserId() == null) {
            return badRequest("사용자를 선택해주세요.");
        }

        Optional<User> user = userRepository.findById(request.getUserId());
        if(user.isEmpty()) {
            return badRequest("존재하지 않는 사용자입니다.");
        }

        AppUser saved = appUserService.grantApplicationAdmin(application.get().getApplicationId(), user.get().getId());

        return new ResponseEntity<>(ApplicationAdminResponse.of(saved, user.get()), HttpStatus.OK);
    }

    private ResponseEntity<?> badRequest(String message) {
        return new ResponseEntity<>(Map.of("message", message), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<?> notFound(String message) {
        return new ResponseEntity<>(Map.of("message", message), HttpStatus.NOT_FOUND);
    }
}
