package com.prometis.appbuilder.app.datasource;

import lombok.Getter;

import java.util.Objects;

/**
 * 등록된 데이터소스(DB/REST 서버/파일저장소/알림)를 찾는 키.
 * 데이터소스 이름은 애플리케이션 안에서만 유일하므로 애플리케이션ID 와 함께 찾는다.
 */
@Getter
public class LookupKey {
    private String applicationId;
    private String dataSourceName;

    public static LookupKey generateKey(String applicationId, String dataSourceName) {
        LookupKey lookupKey = new LookupKey();
        lookupKey.applicationId = applicationId;
        lookupKey.dataSourceName = dataSourceName;

        return lookupKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        LookupKey lookupKey = (LookupKey) obj;

        return Objects.equals(applicationId, lookupKey.applicationId)
                && Objects.equals(dataSourceName, lookupKey.dataSourceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, dataSourceName);
    }
}
