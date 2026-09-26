package com.prometis.appbuilder.app.code;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommonCodeKindRepository extends JpaRepository<CommonCodeKind,Long> {
    Optional<CommonCodeKind> findByCode(String codeKindCode);
}
