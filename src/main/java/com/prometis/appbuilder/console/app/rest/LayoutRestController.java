package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.image.ImageService;
import com.prometis.appbuilder.app.view.LayoutRepository;
import com.prometis.appbuilder.app.view.domain.Layout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("console.LayoutRestController")
@RequestMapping("/{applicationId}/console/api/layout")
public class LayoutRestController {
    @Autowired
    private LayoutRepository repository;

    // pages/index 사이드바에 들어가는 크기라 너무 작거나 크지 않게 막는다
    private static final int MIN_PROFILE_IMG_SIZE = 16;
    private static final int MAX_PROFILE_IMG_SIZE = 200;
    // Layout.loginImgLogoLink 컬럼 길이
    private static final int MAX_LOGIN_IMG_LOGO_LINK_LENGTH = 500;

    @GetMapping("")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId) {
        List<Layout> results = repository.findByApplicationId(applicationId).map(List::of).orElse(List.of());

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        Layout results = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        String profileImg = profileImgOf(params);
        if(profileImg != null && !ImageService.isUserImageLink(applicationId, profileImg)) {
            return invalidProfileImg(applicationId);
        }

        Integer profileImgSize;
        String loginImgLogoLink;
        try {
            profileImgSize = profileImgSizeOf(params);
            loginImgLogoLink = loginImgLogoLinkOf(params);
        }
        catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }

        Layout createdData = Layout.builder()
                .applicationId(applicationId)
                .useCompanySelect(Boolean.valueOf((String) params.get("useCompanySelect")))
                .useProfile(Boolean.valueOf((String) params.get("useProfile")))
                .useLogo(Boolean.valueOf((String) params.get("useLogo")))
                .logoText((String) params.get("logoText"))
                .logoBackgroundColor((String) params.get("logoBackgroundColor"))
                .logoTextColor((String) params.get("logoTextColor"))
                .logoLink((String) params.get("logoLink"))
                .logoImg((String) params.get("logoImg"))
                .layoutTitle((String) params.get("layoutTitle"))
                .homeObjectCode((String) params.get("homeObjectCode"))
                .faviconPath((String) params.get("faviconPath"))
                .profileImg(profileImg)
                .profileImgSize(profileImgSize)
                .loginImgLogoLink(loginImgLogoLink)
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        Layout savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        String profileImg = profileImgOf(params);
        if(profileImg != null && !ImageService.isUserImageLink(applicationId, profileImg)) {
            return invalidProfileImg(applicationId);
        }

        Integer profileImgSize;
        String loginImgLogoLink;
        try {
            profileImgSize = profileImgSizeOf(params);
            loginImgLogoLink = loginImgLogoLinkOf(params);
        }
        catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }

        savedData.update(
                Boolean.valueOf((String) params.get("useCompanySelect")),
                Boolean.valueOf((String) params.get("useProfile")),
                Boolean.valueOf((String) params.get("useLogo")),
                (String) params.get("logoText"),
                (String) params.get("logoBackgroundColor"),
                (String) params.get("logoTextColor"),
                (String) params.get("logoLink"),
                (String) params.get("logoImg"),
                (String) params.get("layoutTitle"),
                (String) params.get("homeObjectCode"),
                (String) params.get("faviconPath"),
                profileImg,
                profileImgSize,
                loginImgLogoLink
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        Layout savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 비어 있으면 프로필 이미지 없음 (기본 이미지)
    private static String profileImgOf(Map<String,Object> params) {
        Object value = params.get("profileImg");
        if(value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();

        return text.isEmpty() ? null : text;
    }

    private static ResponseEntity<?> invalidProfileImg(String applicationId) {
        return error("프로필 이미지 링크는 " + "/" + applicationId + "/image/{storageType}/" + ImageService.USER_ID_EXPRESSION + " 형식이어야 합니다.");
    }

    // 프로필 이미지 크기(px). 비어 있으면 null (AdminLTE 기본 크기)
    private static Integer profileImgSizeOf(Map<String,Object> params) {
        Object value = params.get("profileImgSize");
        String text = value == null ? "" : String.valueOf(value).trim();
        if(text.isEmpty()) {
            return null;
        }

        int size;
        try {
            size = Integer.parseInt(text);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("프로필 이미지 크기는 숫자로 입력해주세요.");
        }

        if(size < MIN_PROFILE_IMG_SIZE || size > MAX_PROFILE_IMG_SIZE) {
            throw new IllegalArgumentException("프로필 이미지 크기는 " + MIN_PROFILE_IMG_SIZE + " ~ " + MAX_PROFILE_IMG_SIZE + "px 로 입력해주세요.");
        }

        return size;
    }

    // 로그인 화면 로고 이미지 링크. 비어 있으면 null (애플리케이션 이름을 글자로 보여준다)
    private static String loginImgLogoLinkOf(Map<String,Object> params) {
        Object value = params.get("loginImgLogoLink");
        String text = value == null ? "" : String.valueOf(value).trim();
        if(text.isEmpty()) {
            return null;
        }

        if(text.length() > MAX_LOGIN_IMG_LOGO_LINK_LENGTH) {
            throw new IllegalArgumentException("로그인 이미지 로고 링크는 " + MAX_LOGIN_IMG_LOGO_LINK_LENGTH + "자까지 입력할 수 있습니다.");
        }

        return text;
    }

    private static ResponseEntity<?> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
