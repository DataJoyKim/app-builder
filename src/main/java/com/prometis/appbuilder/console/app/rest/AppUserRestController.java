package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserService;
import com.prometis.appbuilder.console.app.dto.AppUserCandidateResponse;
import com.prometis.appbuilder.console.app.dto.AppUserRegisterRequest;
import com.prometis.appbuilder.console.app.dto.AppUserResponse;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 애플리케이션 사용자(AppUser) 등록. 플랫폼 사용자(User)에 있는 사람만 등록할 수 있다.
 */
@RestController
@RequestMapping("/{applicationId}/console/api/app-user")
public class AppUserRestController {
    @Autowired
    private AppUserService appUserService;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId) {
        List<AppUser> appUsers = appUserService.getAppUsers(applicationId);

        Map<Long, User> users = userRepository.findAllById(appUsers.stream().map(AppUser::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<AppUserResponse> results = appUsers.stream()
                .map(appUser -> AppUserResponse.of(appUser, users.get(appUser.getUserId())))
                .toList();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    // 아직 이 애플리케이션에 등록되지 않은 사용자
    @GetMapping("/candidates")
    public ResponseEntity<?> getCandidates(@PathVariable("applicationId") String applicationId) {
        Set<Long> registered = appUserService.getAppUsers(applicationId).stream()
                .map(AppUser::getUserId)
                .collect(Collectors.toSet());

        List<AppUserCandidateResponse> results = userRepository.findAll().stream()
                .filter(user -> !registered.contains(user.getId()))
                .map(AppUserCandidateResponse::of)
                .toList();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> register(@PathVariable("applicationId") String applicationId, @RequestBody AppUserRegisterRequest request) {
        List<Long> userIds = request.getUserIds() == null ? List.of() : request.getUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if(userIds.isEmpty()) {
            return badRequest("등록할 사용자를 선택해주세요.");
        }

        // User 에 없는 대상은 등록하지 않는다
        Set<Long> existing = userRepository.findAllById(userIds).stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        if(existing.size() != userIds.size()) {
            return badRequest("존재하지 않는 사용자가 포함되어 있습니다.");
        }

        List<AppUser> registered = appUserService.registerAppUsers(applicationId, userIds);

        return new ResponseEntity<>(Map.of(
                "registered", registered.size(),
                "skipped", userIds.size() - registered.size()
        ), HttpStatus.OK);
    }

    private ResponseEntity<?> badRequest(String message) {
        return new ResponseEntity<>(Map.of("message", message), HttpStatus.BAD_REQUEST);
    }
}
