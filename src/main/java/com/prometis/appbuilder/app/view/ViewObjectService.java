package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.app.security.domain.GrantedAuthority;
import com.prometis.appbuilder.app.view.domain.*;
import com.prometis.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ViewObjectService {
    private final ViewObjectRepository viewObjectRepository;
    private final MenuRepository menuRepository;
    private final MenuAuthorityRepository menuAuthorityRepository;
    private final ViewObjectContentRepository viewObjectContentRepository;
    private final ViewActionRepository viewActionRepository;
    private final ViewCodeRepository viewCodeRepository;

    public ViewObject getViewObject(String objectCode) {
        Optional<ViewObject> optionalViewObject = viewObjectRepository.findByObjectCode(objectCode);
        if(optionalViewObject.isEmpty()) {
            return null;
        }

        return optionalViewObject.get();
    }

    public void validateAuthorization(AuthenticatedUser user, ViewObject accessViewObject) throws BusinessException {
        List<GrantedAuthority> grantedAuthorityList = user.getGrantedAuthorities();
        if(grantedAuthorityList.isEmpty()) {
            throw new BusinessException(MenuErrorMessage.NOT_HAS_AUTHORITIES);
        }

        List<Menu> menuList = menuRepository.findByViewObject(accessViewObject);

        // 상위 메뉴 가져오기
        List<Menu> rootMenuList = new ArrayList<>();
        for(Menu menu : menuList) {
            Menu rootMenu = menu;
            while (rootMenu.getParentMenu() != null) {
                rootMenu = rootMenu.getParentMenu();
            }

            rootMenuList.add(rootMenu);
        }

        // ROOT 메뉴에 매핑된 권한 가져오기
        Map<String, MenuAuthority> menuAuthorityMap = new HashMap<>();
        for(Menu rootMenu : rootMenuList) {
            List<MenuAuthority> menuAuthorities = menuAuthorityRepository.findByMenu(rootMenu);

            for(MenuAuthority menuAuthority : menuAuthorities) {
                menuAuthorityMap.put(menuAuthority.getAuthorityCode(), menuAuthority);
            }
        }

        if(menuAuthorityMap.isEmpty()) {
            throw new BusinessException(MenuErrorMessage.NOT_SETTING_AUTHORITY);
        }

        // 내가 가진 권한 검증
        boolean hasAuthority = false;
        for(GrantedAuthority grantedAuthority : grantedAuthorityList) {
            if(menuAuthorityMap.containsKey(grantedAuthority.getRole())) {
                hasAuthority = true;
                break;
            }
        }

        if(!hasAuthority) {
            throw new BusinessException(MenuErrorMessage.PERMISSION_DENIED);
        }
    }

    public ViewObjectContent getViewObjectContent(String objectCode) {
        return viewObjectContentRepository.findByObjectCode(objectCode)
                .orElseThrow();
    }

    public List<ViewAction> getViewActions(String objectCode) {
        return viewActionRepository.findByObjectCode(objectCode);
    }

    public List<ViewCode> getViewCodes(String objectCode) {
        return viewCodeRepository.findByObjectCode(objectCode);
    }
}
