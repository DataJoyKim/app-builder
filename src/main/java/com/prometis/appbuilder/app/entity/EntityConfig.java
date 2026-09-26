package com.prometis.appbuilder.app.entity;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class EntityConfig {
    private String statusParamKeyName = "_status";
    private String seqParamKeyName = "_seq";
}
