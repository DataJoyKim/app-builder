package com.prometis.appbuilder.app.image;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 앱에서 쓰는 이미지 링크. 로그인 화면 로고처럼 인증 전에도 보여야 하므로 인증 없이 조회한다.
 */
@RestController
@RequestMapping("/{applicationId}/image")
public class ImageController {
    @Autowired
    private ImageService imageService;

    @GetMapping("/{storageType}/{filename}")
    public ResponseEntity<?> getFileContent(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @PathVariable(name = "applicationId") String applicationId,
            @PathVariable(name = "storageType") String storageType,
            @PathVariable(name = "filename") String filename
    ) {
        Optional<ImageInfo> imageOptional = imageService.findImage(applicationId, storageType, filename);
        if(imageOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ImageInfo image = imageOptional.get();

        // 같은 파일명으로 지우고 다시 올리면 id 가 바뀌므로 id 를 ETag 로 쓴다. 바뀌지 않았으면 내용(LOB)을 읽지 않고 304
        String etag = "\"" + image.id() + "\"";
        if(new ServletWebRequest(httpRequest, httpResponse).checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        Optional<byte[]> contentOptional = imageService.findContent(image.id());
        if(contentOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imageService.contentTypeOf(image.fileExtension())))
                .eTag(etag)
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(image.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff");

        // SVG 는 스크립트를 담을 수 있으므로 직접 열었을 때 실행되지 않게 막는다 (<img> 로 쓰는 데는 영향 없음)
        if("svg".equalsIgnoreCase(image.fileExtension())) {
            builder.header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox");
        }

        return builder.body(contentOptional.get());
    }
}
