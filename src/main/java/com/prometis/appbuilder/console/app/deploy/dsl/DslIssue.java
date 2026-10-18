package com.prometis.appbuilder.console.app.deploy.dsl;

/**
 * DSL 검증 결과 하나. ERROR 가 하나라도 있으면 커밋할 수 없고, WARNING 은 알리기만 한다.
 */
public record DslIssue(Level level, String path, String message) {
    public enum Level {
        ERROR, WARNING
    }

    public static DslIssue error(String path, String message) {
        return new DslIssue(Level.ERROR, path, message);
    }

    public static DslIssue warning(String path, String message) {
        return new DslIssue(Level.WARNING, path, message);
    }

    public boolean isError() {
        return Level.ERROR.equals(level);
    }
}
