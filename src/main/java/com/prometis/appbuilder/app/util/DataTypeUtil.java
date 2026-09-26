package com.prometis.appbuilder.app.util;

public class DataTypeUtil {
    public static Long valueLongOf(Object value) {
        Long id = null;
        if(value instanceof Long) {
            id = (Long) value;
        }
        else if(value instanceof Integer) {
            id = Long.valueOf((Integer) value);
        }

        return id;
    }

    // 화면에서 넘어오는 숫자는 Integer 로 올 때도, 소수점이 붙어 Double 로 올 때도 있다.
    public static Integer valueIntegerOf(Object value) {
        if(value instanceof Number) {
            return ((Number) value).intValue();
        }

        return null;
    }
}
