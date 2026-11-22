package com.prometis.appbuilder.app.view.log;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ViewAccessLogRepository extends JpaRepository<ViewAccessLog, Long>, JpaSpecificationExecutor<ViewAccessLog> {
    // 보관 기간 정리(LogCleanupService). 한 문장으로 지우므로 엔티티를 읽어 오지 않는다
    @Modifying
    @Query("delete from ViewAccessLog l where l.accessedDate < :before")
    int deleteByDateBefore(@Param("before") LocalDate before);

    // [accessedDate, accessResult, count]
    @Query("select l.accessedDate, l.accessResult, count(l) from ViewAccessLog l " +
            "where l.applicationId = :applicationId and l.accessedDate >= :from " +
            "group by l.accessedDate, l.accessResult")
    List<Object[]> countDaily(@Param("applicationId") String applicationId, @Param("from") LocalDate from);

    @Query("select count(distinct l.userId) from ViewAccessLog l " +
            "where l.applicationId = :applicationId and l.accessedDate >= :from and l.accessResult = :result")
    long countUsers(@Param("applicationId") String applicationId, @Param("from") LocalDate from,
                    @Param("result") ViewAccessResult result);

    // [objectCode, 화면 이름, 접근 수] 접근 수 많은 순
    @Query("select l.objectCode, max(l.objectName), count(l) from ViewAccessLog l " +
            "where l.applicationId = :applicationId and l.accessedDate >= :from and l.accessResult = :result " +
            "group by l.objectCode order by count(l) desc")
    List<Object[]> countByView(@Param("applicationId") String applicationId, @Param("from") LocalDate from,
                               @Param("result") ViewAccessResult result, Pageable pageable);

    // [menuCd, 메뉴 이름, 접근 수] 접근 수 많은 순
    @Query("select l.menuCd, max(l.menuNm), count(l) from ViewAccessLog l " +
            "where l.applicationId = :applicationId and l.accessedDate >= :from and l.menuCd is not null " +
            "and l.accessResult = :result " +
            "group by l.menuCd order by count(l) desc")
    List<Object[]> countByMenu(@Param("applicationId") String applicationId, @Param("from") LocalDate from,
                               @Param("result") ViewAccessResult result, Pageable pageable);
}
