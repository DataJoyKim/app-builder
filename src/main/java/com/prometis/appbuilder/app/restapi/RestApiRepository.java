package com.prometis.appbuilder.app.restapi;

import com.prometis.appbuilder.app.restapi.code.HttpMethodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestApiRepository extends JpaRepository<RestApi, Long> {
    Optional<RestApi> findByApiCode(String apiCode);

    List<RestApi> findByHttpMethod(HttpMethodType httpMethod);
}
