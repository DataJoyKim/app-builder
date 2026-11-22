package com.prometis.appbuilder.app.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 실행/접근 로그를 저장한다. 로그는 본 처리와 별개라서
 * - 호출한 쪽의 트랜잭션이 롤백돼도 남도록 새 트랜잭션(REQUIRES_NEW)에서 저장하고,
 * - 저장이 실패해도 본 처리(워크플로우 응답, 화면 응답)에 영향을 주지 않도록 예외를 삼키고 경고만 남긴다.
 */
@Slf4j
@Component
public class AccessLogWriter {
    private final TransactionTemplate transactionTemplate;

    public AccessLogWriter(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void write(String kind, Runnable save) {
        try {
            transactionTemplate.executeWithoutResult(status -> save.run());
        }
        catch (RuntimeException e) {
            log.warn("{} 로그를 저장하지 못했습니다: {}", kind, e.toString());
        }
    }

    public static String truncate(String value, int max) {
        if(value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
