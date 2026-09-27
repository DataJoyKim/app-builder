package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.ViewAction;
import com.prometis.appbuilder.app.view.domain.ViewCode;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import com.prometis.appbuilder.app.view.domain.ViewObjectContent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ViewObjectService {
    private final ViewObjectRepository viewObjectRepository;
    private final ViewObjectContentRepository viewObjectContentRepository;
    private final ViewActionRepository viewActionRepository;
    private final ViewCodeRepository viewCodeRepository;

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
}
