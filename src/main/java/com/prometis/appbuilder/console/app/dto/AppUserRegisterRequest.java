package com.prometis.appbuilder.console.app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AppUserRegisterRequest {
    private List<Long> userIds;
}
