package com.prometis.appbuilder.platform.join;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppJoinSettingResponse {
    private String joinPolicy;
    // 지워진 그룹이면 null
    private Long defaultUserGroupId;
}
