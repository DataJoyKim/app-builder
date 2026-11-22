package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.log.AccessLogWriter;
import com.prometis.appbuilder.app.log.AccessUserResolver;
import com.prometis.appbuilder.app.util.ClientIpResolver;
import com.prometis.appbuilder.app.view.domain.Menu;
import com.prometis.appbuilder.app.view.domain.ViewAction;
import com.prometis.appbuilder.app.view.domain.ViewCode;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import com.prometis.appbuilder.app.view.domain.ViewObjectContent;
import com.prometis.appbuilder.app.view.log.ViewAccessLog;
import com.prometis.appbuilder.app.view.log.ViewAccessLogRepository;
import com.prometis.appbuilder.app.view.log.ViewAccessResult;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ViewObjectService {
    private final ViewObjectRepository viewObjectRepository;
    private final ViewObjectContentRepository viewObjectContentRepository;
    private final ViewActionRepository viewActionRepository;
    private final ViewCodeRepository viewCodeRepository;
    private final MenuRepository menuRepository;
    private final ViewAccessLogRepository viewAccessLogRepository;
    private final AccessUserResolver accessUserResolver;
    private final AccessLogWriter accessLogWriter;

    public ViewObject getViewObject(String applicationId, String objectCode) {
        Optional<ViewObject> optionalViewObject = viewObjectRepository.findByApplicationIdAndObjectCode(applicationId, objectCode);
        return optionalViewObject.orElse(null);

    }

    public ViewObjectContent getViewObjectContent(String applicationId, String objectCode) {
        return viewObjectContentRepository.findByApplicationIdAndObjectCode(applicationId, objectCode)
                .orElseThrow();
    }

    public List<ViewAction> getViewActions(String applicationId, String objectCode) {
        return viewActionRepository.findByApplicationIdAndObjectCode(applicationId, objectCode);
    }

    public List<ViewCode> getViewCodes(String applicationId, String objectCode) {
        return viewCodeRepository.findByApplicationIdAndObjectCode(applicationId, objectCode);
    }

    /**
     * 화면 접근 로그. 누가(user, 없으면 로그인 쿠키로 확인) 언제 어떤 메뉴(menuCd)에서 어떤 화면(viewObject)에 접근했고 결과가 어땠는지 남긴다.
     * menuCd 는 앱 화면의 메뉴 링크가 쿼리로 붙여 보내는 값이라, 이 애플리케이션에 있는 메뉴일 때만 남긴다.
     * 로그 저장이 실패해도 화면은 그대로 연다.
     */
    public void recordAccess(
            HttpServletRequest request,
            String applicationId,
            ViewObject viewObject,
            String menuCd,
            AuthenticatedUser user,
            ViewAccessResult result
    ) {
        accessLogWriter.write("화면 접근", () -> {
            Menu menu = (menuCd == null || menuCd.isBlank())
                    ? null
                    : menuRepository.findByApplicationIdAndMenuCd(applicationId, menuCd).orElse(null);
            AuthenticatedUser actor = accessUserResolver.resolve(request, user);
            LocalDateTime now = LocalDateTime.now();

            viewAccessLogRepository.save(ViewAccessLog.builder()
                    .applicationId(applicationId)
                    .objectCode(viewObject.getObjectCode())
                    .objectName(viewObject.getObjectName())
                    .objectType(viewObject.getType() == null ? null : viewObject.getType().name())
                    .menuCd(menu == null ? null : menu.getMenuCd())
                    .menuNm(menu == null ? null : menu.getMenuNm())
                    .userId(actor == null ? null : actor.getUserId())
                    .userName(actor == null ? null : AccessLogWriter.truncate(actor.getUserName(), 200))
                    .clientIp(AccessLogWriter.truncate(ClientIpResolver.resolve(request), 64))
                    .accessResult(result)
                    .accessedAt(now)
                    .accessedDate(now.toLocalDate())
                    .build());
        });
    }
}
