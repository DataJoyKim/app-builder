package com.prometis.appbuilder.restapi;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestApiParameterRepository extends JpaRepository<RestApiParameter, Long> {
    List<RestApiParameter> findByRestApiIdOrderByOrderNum(Long restApiId);

    void deleteByRestApiId(Long restApiId);
}
