package com.prometis.appbuilder.app.datasource.restserver;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Table(uniqueConstraints = {@UniqueConstraint(name="DATA_SOURCE_REST_SERVER_UQ",columnNames={"DATA_SOURCE_NAME"})})
@Entity
public class DataSourceRestServer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String dataSourceName;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(length = 500)
    private String note;

    @Column(nullable = false, length = 300)
    private String baseUrl;
    @Column
    private Integer connectTimeout;
    @Column
    private Integer connectRequestTimeout;
    @Column
    private Integer connectionMaxTotal;
    @Column
    private Integer connectionDefaultMaxPerRoute;

    public RestClient createDataSource() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(connectionMaxTotal);
        connectionManager.setDefaultMaxPerRoute(connectionDefaultMaxPerRoute);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setConnectionRequestTimeout(connectRequestTimeout);
        factory.setHttpClient(httpClient);

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .requestInterceptor(new RequestLoggingInterceptor())
                .build();
    }
}
