package com.prometis.appbuilder.app.code;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommonCodeRepository extends JpaRepository<CommonCode, Long> {
    @Query("""
        SELECT c
        FROM CommonCode c
        LEFT JOIN c.codeKind k
        WHERE k.code IN :codeKindCodes
    """)
    List<CommonCode> findByCodeKindCodes(@Param("codeKindCodes") List<String> codeKindCodes);

    List<CommonCode> findByCodeKind(CommonCodeKind codeKind);
}
