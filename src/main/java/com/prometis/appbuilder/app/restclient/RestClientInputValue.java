package com.prometis.appbuilder.app.restclient;

import com.prometis.appbuilder.app.restclient.code.ValueType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

public class RestClientInputValue {

    public static ResolvedValue resolve(
            ValueType valueType,
            String key,
            String inputValue,
            Map<String,Object> params
    ) {
        if(ValueType.PARAM_VALUE.equals(valueType)) {
            return new ResolvedValue(key, params.get(key));
        }
        else if(ValueType.INPUT_VALUE.equals(valueType)) {
            return new ResolvedValue(key, inputValue);
        }
        else {
            throw new RuntimeException("ValueType is null...");
        }
    }

    @Getter @AllArgsConstructor
     static class ResolvedValue {
        private String key;
        private Object value;
    }
}
