package com.prometis.appbuilder.console.platform.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApplicationRequest {
    private String applicationId;
    private String name;
    private String status;
    private String description;
}
