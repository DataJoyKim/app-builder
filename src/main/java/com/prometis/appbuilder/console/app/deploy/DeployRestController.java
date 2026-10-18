package com.prometis.appbuilder.console.app.deploy;

import com.prometis.appbuilder.console.app.deploy.dto.DeployCommitRequest;
import com.prometis.appbuilder.console.app.deploy.dto.DeployGitSettingRequest;
import com.prometis.appbuilder.console.app.deploy.github.GitHubException;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.service.AuthenticationService;
import com.prometis.appbuilder.security.token.TokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 애플리케이션 콘솔 > Deploy > Commit 화면(console/app/contents/deploy-commit.html)의 API.
 * GitHub 저장소 연동 설정, 지금 메타데이터의 DSL 보기, 저장소와 비교, 커밋(push).
 */
@Slf4j
@RestController
@RequestMapping("/{applicationId}/console/api/deploy")
@RequiredArgsConstructor
public class DeployRestController {
    private final DeployService deployService;
    private final AuthenticationService authenticationService;

    @GetMapping("/setting")
    public ResponseEntity<?> getSetting(@PathVariable("applicationId") String applicationId) {
        return ResponseEntity.ok(deployService.getSetting(applicationId));
    }

    @PutMapping("/setting")
    public ResponseEntity<?> saveSetting(@PathVariable("applicationId") String applicationId, @RequestBody DeployGitSettingRequest request) {
        return ResponseEntity.ok(deployService.saveSetting(applicationId, request));
    }

    @DeleteMapping("/setting")
    public ResponseEntity<?> deleteSetting(@PathVariable("applicationId") String applicationId) {
        deployService.deleteSetting(applicationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/dsl")
    public ResponseEntity<?> getDsl(@PathVariable("applicationId") String applicationId) {
        return ResponseEntity.ok(deployService.getDsl(applicationId));
    }

    @PostMapping("/compare")
    public ResponseEntity<?> compare(@PathVariable("applicationId") String applicationId) {
        return ResponseEntity.ok(deployService.compare(applicationId));
    }

    @PostMapping("/commit")
    public ResponseEntity<?> commit(
            @PathVariable("applicationId") String applicationId,
            @RequestBody DeployCommitRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(deployService.commit(applicationId, request, loginUserNameOf(httpRequest)));
    }

    @ExceptionHandler(DeployException.class)
    public ResponseEntity<?> handleDeployException(DeployException e) {
        return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.valueOf(e.getStatus()));
    }

    // GitHub 가 거절한 요청(토큰/권한/저장소 문제)은 400, 연결 실패나 GitHub 서버 오류는 502 로 보낸다
    @ExceptionHandler(GitHubException.class)
    public ResponseEntity<?> handleGitHubException(GitHubException e) {
        log.warn("GitHub 요청 실패: {}", e.getMessage());
        HttpStatus status = (e.getStatus() >= 400 && e.getStatus() < 500) ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
        return new ResponseEntity<>(Map.of("message", e.getMessage()), status);
    }

    private String loginUserNameOf(HttpServletRequest httpRequest) {
        try {
            return authenticationService.authentication(TokenCookie.resolveAccessToken(httpRequest)).getUserName();
        }
        catch (SecurityBusinessException e) {
            return null;
        }
    }
}
