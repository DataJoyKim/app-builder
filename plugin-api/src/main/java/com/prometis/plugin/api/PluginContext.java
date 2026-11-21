package com.prometis.plugin.api;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * 호스트(admin-builder)가 플러그인 실행 중에 열어주는 기능.
 * 모든 조회는 이 실행이 속한 애플리케이션 범위로 제한된다 (다른 애플리케이션의 데이터소스는 보이지 않는다).
 */
public interface PluginContext {

    /** 플러그인을 실행한 애플리케이션ID */
    String getApplicationId();

    /**
     * 애플리케이션 콘솔 > DataSource > Database 에 등록된 데이터소스를 DataSource 명으로 찾는다.
     * 커넥션 풀은 호스트가 관리하므로 플러그인은 꺼낸 Connection 을 반드시 close 해서(try-with-resources) 풀에 돌려줘야 한다.
     * DataSource 자체를 close 해서는 안 된다.
     */
    Optional<DataSource> findDataSource(String dataSourceName);

    /**
     * {@link #findDataSource(String)} 와 같지만 없으면 {@link IllegalArgumentException} 을 던진다.
     */
    default DataSource getDataSource(String dataSourceName) {
        return findDataSource(dataSourceName)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 데이터소스입니다. [" + dataSourceName + "]"));
    }
}
