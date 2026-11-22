package com.prometis.appbuilder.console.app.log;

import com.prometis.appbuilder.app.view.log.ViewAccessLog;
import com.prometis.appbuilder.app.view.log.ViewAccessLogRepository;
import com.prometis.appbuilder.app.view.log.ViewAccessResult;
import com.prometis.appbuilder.app.workflow.code.WorkflowTrigger;
import com.prometis.appbuilder.app.workflow.log.WorkflowExecutionLog;
import com.prometis.appbuilder.app.workflow.log.WorkflowExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 애플리케이션 콘솔 > Log 화면의 로그 조회. 항상 그 애플리케이션 로그만, 최근 것부터 페이지로 돌려준다.
 * 글자 조건은 대소문자 구분 없는 부분 일치다.
 */
@Service
@RequiredArgsConstructor
public class LogSearchService {
    static final int DEFAULT_PAGE_SIZE = 50;
    static final int MAX_PAGE_SIZE = 200;

    private final WorkflowExecutionLogRepository workflowExecutionLogRepository;
    private final ViewAccessLogRepository viewAccessLogRepository;

    /**
     * @param from       실행일 시작 (포함, 비우면 제한 없음)
     * @param to         실행일 끝 (포함, 비우면 제한 없음)
     * @param workflow   워크플로우 코드
     * @param object     워크플로우를 부른 화면 코드
     * @param user       사용자 이름 (숫자면 사용자 ID 로도 찾는다)
     * @param resultType SUCCESS / ERROR
     * @param trigger    WEB / REST_API / SCHEDULER
     */
    @Transactional(readOnly = true)
    public LogPage<WorkflowExecutionLog> workflowLogs(
            String applicationId, LocalDate from, LocalDate to,
            String workflow, String object, String user, String resultType, String trigger,
            Integer page, Integer size
    ) {
        Specification<WorkflowExecutionLog> spec = Specification.<WorkflowExecutionLog>where(equal("applicationId", applicationId))
                .and(dateRange("executedDate", from, to))
                .and(contains(workflow, "workflowCode"))
                .and(contains(object, "objectCode"))
                .and(user(user))
                .and(equal("resultType", blankToNull(resultType)))
                .and(equal("triggerType", parse(WorkflowTrigger.class, trigger)));

        return LogPage.of(workflowExecutionLogRepository.findAll(spec, pageRequest(page, size, "executedAt")));
    }

    /**
     * @param object 화면 코드 또는 화면 이름
     * @param menu   메뉴 코드 또는 메뉴 이름
     * @param result ALLOWED / UNAUTHENTICATED / FORBIDDEN
     */
    @Transactional(readOnly = true)
    public LogPage<ViewAccessLog> viewLogs(
            String applicationId, LocalDate from, LocalDate to,
            String object, String menu, String user, String result,
            Integer page, Integer size
    ) {
        Specification<ViewAccessLog> spec = Specification.<ViewAccessLog>where(equal("applicationId", applicationId))
                .and(dateRange("accessedDate", from, to))
                .and(contains(object, "objectCode", "objectName"))
                .and(contains(menu, "menuCd", "menuNm"))
                .and(user(user))
                .and(equal("accessResult", parse(ViewAccessResult.class, result)));

        return LogPage.of(viewAccessLogRepository.findAll(spec, pageRequest(page, size, "accessedAt")));
    }

    private static PageRequest pageRequest(Integer page, Integer size, String timeField) {
        int pageNo = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(pageNo, pageSize, Sort.by(Sort.Order.desc(timeField), Sort.Order.desc("id")));
    }

    private static <T> Specification<T> equal(String field, Object value) {
        if(value == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    private static <T> Specification<T> dateRange(String field, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if(from != null && to != null) return cb.between(root.get(field), from, to);
            if(from != null) return cb.greaterThanOrEqualTo(root.get(field), from);
            if(to != null) return cb.lessThanOrEqualTo(root.get(field), to);
            return null;
        };
    }

    // 여러 필드 중 하나라도 키워드를 포함하면 맞는다
    private static <T> Specification<T> contains(String keyword, String... fields) {
        String kw = blankToNull(keyword);
        if(kw == null) {
            return null;
        }
        String pattern = "%" + escapeLike(kw.toLowerCase()) + "%";
        return (root, query, cb) -> cb.or(List.of(fields).stream()
                .map(field -> cb.like(cb.lower(root.get(field)), pattern, '\\'))
                .toArray(jakarta.persistence.criteria.Predicate[]::new));
    }

    private static <T> Specification<T> user(String keyword) {
        String kw = blankToNull(keyword);
        if(kw == null) {
            return null;
        }
        Specification<T> byName = contains(kw, "userName");
        if(kw.matches("\\d{1,18}")) {
            Long userId = Long.valueOf(kw);
            return byName.or((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        return byName;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        String v = blankToNull(value);
        if(v == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, v);
        }
        catch (IllegalArgumentException e) {
            throw new LogSearchException("알 수 없는 조건 값입니다: " + v);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public record LogPage<T>(List<T> content, long totalElements, int totalPages, int page, int size) {
        static <T> LogPage<T> of(Page<T> page) {
            return new LogPage<>(page.getContent(), page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
        }
    }

    public static class LogSearchException extends RuntimeException {
        public LogSearchException(String message) {
            super(message);
        }
    }
}
