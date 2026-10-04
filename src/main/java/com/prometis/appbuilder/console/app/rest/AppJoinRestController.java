package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.platform.join.AppMembershipService;
import com.prometis.appbuilder.platform.join.JoinException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 애플리케이션 가입 설정과 가입 신청. 콘솔의 사용자 화면에서 쓴다.
 * - setting : 가입 방식(INVITE_ONLY/APPROVAL/OPEN) + 새 사용자를 넣을 기본 사용자 그룹. 앱 가입 페이지는 /{applicationId}/signup
 * - request : 승인 가입(APPROVAL)의 대기 중인 신청. 승인하면 사용자(APPLICATION_USER)로 등록하고, 거절하면 지운다
 */
@RestController
@RequestMapping("/{applicationId}/console/api/app-join")
public class AppJoinRestController {
    @Autowired
    private AppMembershipService appMembershipService;

    @GetMapping("/setting")
    public ResponseEntity<?> getSetting(@PathVariable("applicationId") String applicationId) {
        return new ResponseEntity<>(appMembershipService.getSetting(applicationId), HttpStatus.OK);
    }

    @PutMapping("/setting")
    public ResponseEntity<?> saveSetting(@PathVariable("applicationId") String applicationId, @RequestBody Map<String, Object> request) {
        Object groupId = request.get("defaultUserGroupId");
        Long defaultUserGroupId;
        try {
            defaultUserGroupId = groupId == null || groupId.toString().isBlank() ? null : Long.valueOf(groupId.toString());
        }
        catch (NumberFormatException e) {
            return badRequest("사용자 그룹이 올바르지 않습니다.");
        }

        try {
            Object joinPolicy = request.get("joinPolicy");
            return new ResponseEntity<>(
                    appMembershipService.saveSetting(applicationId, joinPolicy == null ? null : joinPolicy.toString(), defaultUserGroupId),
                    HttpStatus.OK);
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/request")
    public ResponseEntity<?> getRequests(@PathVariable("applicationId") String applicationId) {
        return new ResponseEntity<>(appMembershipService.getRequests(applicationId), HttpStatus.OK);
    }

    @PostMapping("/request/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        try {
            appMembershipService.approve(applicationId, id);
            return new ResponseEntity<>(Map.of("approved", 1), HttpStatus.OK);
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    @DeleteMapping("/request/{id}")
    public ResponseEntity<?> reject(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        try {
            appMembershipService.reject(applicationId, id);
            return new ResponseEntity<>(Map.of("rejected", 1), HttpStatus.OK);
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    private ResponseEntity<?> badRequest(String message) {
        return new ResponseEntity<>(Map.of("message", message), HttpStatus.BAD_REQUEST);
    }
}
