package com.prometis.appbuilder.util;

import jakarta.persistence.AttributeConverter;

/**
 * 열거형을 이름 그대로 문자열 컬럼에 담는다.
 *
 * @Enumerated(EnumType.STRING) 을 쓰면 Hibernate 가 테이블을 만들 때 허용값 CHECK 제약(col in ('A','B'))을 같이 만든다.
 * ddl-auto update 는 이 제약을 늘려주지 않기 때문에, 열거형에 값을 추가하면 이미 만들어진 DB 에서 저장이 깨진다.
 * 컨버터로 담으면 일반 문자열 컬럼이라 이런 제약이 생기지 않는다. 값이 늘어날 수 있는 열거형 컬럼에는 이걸 쓴다.
 *
 * 사용: 열거형 안에 public static class Converter extends EnumStringConverter<그열거형> 을 두고
 *      필드에 @Convert(converter = 그열거형.Converter.class) 를 붙인다.
 */
public abstract class EnumStringConverter<E extends Enum<E>> implements AttributeConverter<E, String> {
    private final Class<E> type;

    protected EnumStringConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        return (dbData == null || dbData.isBlank()) ? null : Enum.valueOf(type, dbData.trim());
    }
}
