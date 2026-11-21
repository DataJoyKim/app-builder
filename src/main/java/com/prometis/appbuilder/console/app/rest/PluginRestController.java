package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.plugin.PluginException;
import com.prometis.appbuilder.app.plugin.PluginManager;
import com.prometis.appbuilder.app.plugin.PluginProperties;
import com.prometis.appbuilder.app.plugin.PluginView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 애플리케이션 콘솔 > Plugins (console/app/contents/plugin-manage.html).
 */
@RestController("console.PluginRestController")
@RequestMapping("/{applicationId}/console/api/plugin")
@RequiredArgsConstructor
public class PluginRestController {
    private final PluginManager pluginManager;

    // 화면에서 업로드 전에 미리 걸러내도록 제한(application.yml platform.plugin)을 알려준다
    @GetMapping("/policy")
    public ResponseEntity<?> policy() {
        PluginProperties policy = pluginManager.policy();

        Map<String, Object> body = new HashMap<>();
        body.put("maxCount", policy.maxCount());
        body.put("maxFileSize", policy.maxFileSize().toBytes());
        body.put("syncIntervalSeconds", policy.syncInterval().toSeconds());

        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @GetMapping("")
    public ResponseEntity<?> getPlugins(@PathVariable("applicationId") String applicationId) {
        return new ResponseEntity<>(pluginManager.getPlugins(applicationId), HttpStatus.OK);
    }

    // 워크플로우 PLUGIN 노드에서 고르는 pluginPath 목록
    @GetMapping("/path")
    public ResponseEntity<?> getPaths(@PathVariable("applicationId") String applicationId) {
        return new ResponseEntity<>(pluginManager.getPaths(applicationId), HttpStatus.OK);
    }

    @GetMapping("/{pluginId}")
    public ResponseEntity<?> getPlugin(@PathVariable("applicationId") String applicationId, @PathVariable("pluginId") String pluginId) {
        return handle(() -> pluginManager.getPlugin(applicationId, pluginId));
    }

    // multipart/form-data: file (jar). pluginId 를 주면 그 플러그인의 jar 교체로 보고 jar 의 Plugin-Id 와 같은지 확인한다
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @PathVariable("applicationId") String applicationId,
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam(name = "pluginId", required = false) String pluginId
    ) {
        return handle(() -> pluginManager.register(applicationId, file, pluginId));
    }

    @PostMapping("/{pluginId}/start")
    public ResponseEntity<?> start(@PathVariable("applicationId") String applicationId, @PathVariable("pluginId") String pluginId) {
        return handle(() -> pluginManager.start(applicationId, pluginId));
    }

    @PostMapping("/{pluginId}/stop")
    public ResponseEntity<?> stop(@PathVariable("applicationId") String applicationId, @PathVariable("pluginId") String pluginId) {
        return handle(() -> pluginManager.stop(applicationId, pluginId));
    }

    @PostMapping("/{pluginId}/reload")
    public ResponseEntity<?> reload(@PathVariable("applicationId") String applicationId, @PathVariable("pluginId") String pluginId) {
        return handle(() -> pluginManager.reload(applicationId, pluginId));
    }

    @PutMapping("/{pluginId}/path")
    public ResponseEntity<?> savePaths(
            @PathVariable("applicationId") String applicationId,
            @PathVariable("pluginId") String pluginId,
            @RequestBody List<PluginView.PathRequest> paths
    ) {
        return handle(() -> pluginManager.savePaths(applicationId, pluginId, paths));
    }

    @DeleteMapping("/{pluginId}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("pluginId") String pluginId) {
        return handle(() -> {
            pluginManager.delete(applicationId, pluginId);
            return Map.of();
        });
    }

    private static ResponseEntity<?> handle(Supplier<Object> action) {
        try {
            return new ResponseEntity<>(action.get(), HttpStatus.OK);
        }
        catch (PluginException e) {
            Map<String, Object> body = new HashMap<>();
            body.put("message", e.getMessage());
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
    }
}
