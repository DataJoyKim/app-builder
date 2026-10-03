package com.prometis.appbuilder.console.app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApplicationCreateRequest {
    private String applicationId;
    private String name;
    private String description;
}
