package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserManageException;
import com.prometis.appbuilder.app.security.appuser.AppUserService;
import com.prometis.appbuilder.console.app.dto.AppUserAuthorityRequest;
import com.prometis.appbuilder.console.app.dto.AppUserCandidateResponse;
import com.prometis.appbuilder.console.app.dto.AppUserCompanyRequest;
import com.prometis.appbuilder.console.app.dto.AppUserRegisterRequest;
import com.prometis.appbuilder.console.app.dto.AppUserResponse;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.service.AuthenticationService;
import com.prometis.appbuilder.security.token.TokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 애플리케이션 사용자(AppUser) 등록/권한 변경/삭제. 플랫폼 사용자(User)에 있는 사람만 등록할 수 있다.
 */
@RestController
@RequestMapping("/{applicationId}/console/api/app-user")
public class AppUserRestController {
    @Autowired
    private AppUserService appUserService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticationService authenticationService;

    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId, HttpServletRequest httpRequest) {
        // 화면이 본인 행의 권한 변경/삭제를 막을 수 있도록 본인 여부(me)를 함께 준다
        Long loginUserId = loginUserIdOf(httpRequest);
        List<AppUser> appUsers = appUserService.getAppUsers(applicationId);

        Map<Long, User> users = userRepository.findAllById(appUsers.stream().map(AppUser::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<AppUserResponse> results = appUsers.stream()
                .map(appUser -> AppUserResponse.of(appUser, users.get(appUser.getUserId()), loginUserId))
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

    @PutMapping("/{id}/authority")
    public ResponseEntity<?> changeAuthority(
            @PathVariable("applicationId") String applicationId,
            @PathVariable("id") Long id,
            @RequestBody AppUserAuthorityRequest request,
            HttpServletRequest httpRequest
    ) {
        Long requesterUserId = loginUserIdOf(httpRequest);
        if(requesterUserId == null) {
            return unauthorized();
        }

        try {
            AppUser appUser = appUserService.changeAuthority(applicationId, id, request.getAuthority(), requesterUserId);
            User user = userRepository.findById(appUser.getUserId()).orElse(null);

            return new ResponseEntity<>(AppUserResponse.of(appUser, user, requesterUserId), HttpStatus.OK);
        }
        catch (AppUserManageException e) {
            return badRequest(e.getMessage());
        }
    }

    @PutMapping("/{id}/company")
    public ResponseEntity<?> changeCompany(
            @PathVariable("applicationId") String applicationId,
            @PathVariable("id") Long id,
            @RequestBody AppUserCompanyRequest request,
            HttpServletRequest httpRequest
    ) {
        Long requesterUserId = loginUserIdOf(httpRequest);
        if(requesterUserId == null) {
            return unauthorized();
        }

        try {
            AppUser appUser = appUserService.changeCompany(applicationId, id, request.getCompanyCode());
            User user = userRepository.findById(appUser.getUserId()).orElse(null);

            return new ResponseEntity<>(AppUserResponse.of(appUser, user, requesterUserId), HttpStatus.OK);
        }
        catch (AppUserManageException e) {
            return badRequest(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable("applicationId") String applicationId,
            @PathVariable("id") Long id,
            HttpServletRequest httpRequest
    ) {
        Long requesterUserId = loginUserIdOf(httpRequest);
        if(requesterUserId == null) {
            return unauthorized();
        }

        try {
            appUserService.deleteAppUser(applicationId, id, requesterUserId);

            return new ResponseEntity<>(Map.of("deleted", 1), HttpStatus.OK);
        }
        catch (AppUserManageException e) {
            return badRequest(e.getMessage());
        }
    }

    // 로그인한 사용자의 userId. 콘솔 필터를 통과했으면 있어야 하지만, 확인할 수 없으면 null
    private Long loginUserIdOf(HttpServletRequest httpRequest) {
        try {
            return authenticationService.authentication(TokenCookie.resolveAccessToken(httpRequest)).getUserId();
        }
        catch (SecurityBusinessException e) {
            return null;
        }
    }

    private ResponseEntity<?> unauthorized() {
        return new ResponseEntity<>(Map.of("message", "로그인이 필요합니다."), HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<?> badRequest(String message) {
        return new ResponseEntity<>(Map.of("message", message), HttpStatus.BAD_REQUEST);
    }
}
