package com.prometis.appbuilder.app.view.dto;

import com.prometis.appbuilder.app.code.CodeResponse;
import com.prometis.appbuilder.app.view.domain.ViewAction;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import com.prometis.appbuilder.app.view.domain.ViewObjectContent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor @Builder
public class ViewDto {
    private ViewObject viewObject;
    private ViewObjectContent viewObjectContent;
    private List<ViewAction> viewActions;
    private Map<String, List<CodeResponse>> codeMap;
}
