package com.prometis.appbuilder.app.plugin;

import com.prometis.appbuilder.app.datasource.LookupKey;
import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseRegister;
import com.prometis.plugin.api.PluginContext;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * 플러그인에 열어주는 호스트 기능. 실행한 애플리케이션의 데이터소스만 보인다.
 */
class HostPluginContext implements PluginContext {
    private final String applicationId;

    HostPluginContext(String applicationId) {
        this.applicationId = applicationId;
    }

    @Override
    public String getApplicationId() {
        return applicationId;
    }

    @Override
    public Optional<DataSource> findDataSource(String dataSourceName) {
        if(dataSourceName == null || dataSourceName.isBlank()) {
            return Optional.empty();
        }

        Map<LookupKey, DataSource> registered = DataSourceDatabaseRegister.getDataSourceMap();
        if(registered == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(registered.get(LookupKey.generateKey(applicationId, dataSourceName)))
                .map(ConnectionOnlyDataSource::new);
    }

    /**
     * 커넥션을 꺼내는 것만 허용한다. 실제 풀(HikariDataSource)을 그대로 넘기면 플러그인이 형변환해서 close 하거나 설정을 바꿀 수 있다.
     */
    private static final class ConnectionOnlyDataSource implements DataSource {
        private final DataSource delegate;

        private ConnectionOnlyDataSource(DataSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return delegate.getConnection();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLFeatureNotSupportedException("플러그인에서는 등록된 계정으로만 연결할 수 있습니다.");
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return delegate.getLoginTimeout();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if(iface.isInstance(this)) {
                return iface.cast(this);
            }
            throw new SQLException("플러그인에서는 데이터소스를 풀 수 없습니다. [" + iface.getName() + "]");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }
    }
}
