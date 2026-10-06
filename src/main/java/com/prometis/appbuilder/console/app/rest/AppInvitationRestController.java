package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.console.app.dto.AppInvitationBatchRequest;
import com.prometis.appbuilder.platform.join.AppInvitationResponse;
import com.prometis.appbuilder.platform.join.AppInvitationService;
import com.prometis.appbuilder.platform.join.JoinException;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.service.AuthenticationService;
import com.prometis.appbuilder.security.token.TokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 애플리케이션 초대. 콘솔의 사용자 화면에서 이메일과 권한(관리자/사용자)을 지정해 초대 링크를 만들고(메일 발송), 목록을 보고, 취소한다.
 * 초대받은 사람은 링크(/{applicationId}/console/join?token=...)에서 가입하거나 기존 계정으로 수락해 초대받은 권한으로 이 애플리케이션에 들어온다.
 */
@RestController
@RequestMapping("/{applicationId}/console/api/app-invitation")
public class AppInvitationRestController {
    private static final int MAX_BATCH_SIZE = 50;

    @Autowired
    private AppInvitationService appInvitationService;
    @Autowired
    private AuthenticationService authenticationService;

    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId) {
        return new ResponseEntity<>(appInvitationService.getInvitations(applicationId, baseUrl()), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(
            @PathVariable("applicationId") String applicationId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        try {
            return new ResponseEntity<>(
                    appInvitationService.create(applicationId, request.get("email"), request.get("authority"), request.get("companyCode"), loginUserIdOf(httpRequest), baseUrl()),
                    HttpStatus.OK);
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    /**
     * 여러 이메일을 한 번에 초대한다. 이메일마다 따로 처리해서, 일부가 잘못되어도 나머지는 초대된다.
     * 같은 이메일(대소문자 무시)은 한 번만 초대한다.
     */
    @PostMapping("/batch")
    public ResponseEntity<?> createAll(
            @PathVariable("applicationId") String applicationId,
            @RequestBody AppInvitationBatchRequest request,
            HttpServletRequest httpRequest
    ) {
        List<String> emails = request.getEmails() == null ? List.of() : request.getEmails().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toMap(email -> email.toLowerCase(Locale.ROOT), email -> email, (first, second) -> first, LinkedHashMap::new))
                .values().stream()
                .toList();

        if(emails.isEmpty()) {
            return badRequest("초대할 이메일을 입력해주세요.");
        }
        if(emails.size() > MAX_BATCH_SIZE) {
            return badRequest("한 번에 " + MAX_BATCH_SIZE + "명까지 초대할 수 있습니다.");
        }

        Long inviterUserId = loginUserIdOf(httpRequest);
        String baseUrl = baseUrl();

        List<Map<String, Object>> results = new ArrayList<>();
        for(String email : emails) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("email", email);
            try {
                AppInvitationResponse invitation = appInvitationService.create(applicationId, email, request.getAuthority(), request.getCompanyCode(), inviterUserId, baseUrl);
                result.put("success", true);
                result.put("link", invitation.getLink());
                result.put("mailSent", invitation.getMailSent());
            }
            catch (JoinException e) {
                result.put("success", false);
                result.put("message", e.getMessage());
            }
            results.add(result);
        }

        long invited = results.stream().filter(result -> Boolean.TRUE.equals(result.get("success"))).count();
        return new ResponseEntity<>(Map.of(
                "invited", invited,
                "failed", results.size() - invited,
                "results", results
        ), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> revoke(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        try {
            appInvitationService.revoke(applicationId, id);
            return new ResponseEntity<>(Map.of("deleted", 1), HttpStatus.OK);
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    // 초대 링크 앞부분: 지금 요청이 들어온 주소 (platform.join.public-base-url 이 있으면 그쪽이 우선)
    private String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
    }

    private Long loginUserIdOf(HttpServletRequest httpRequest) {
        try {
            return authenticationService.authentication(TokenCookie.resolveAccessToken(httpRequest)).getUserId();
        }
        catch (SecurityBusinessException e) {
            return null;
        }
    }

    private ResponseEntity<?> badRequest(String message) {
        return new ResponseEntity<>(Map.of("message", message), HttpStatus.BAD_REQUEST);
    }
}
