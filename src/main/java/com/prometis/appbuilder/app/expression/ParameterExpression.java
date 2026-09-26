package com.prometis.appbuilder.app.expression;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParameterExpression {
    private static final Pattern PATTERN = Pattern.compile("#\\{(\\w+)}");

    public String resolve(String content, Map<String, Object> params) {
        Matcher matcher = PATTERN.matcher(content);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String expressionValue = matcher.group(1);

            Object value = params.get(expressionValue);

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(
                            value != null ? String.valueOf(value) : ""
                    )
            );
        }

        matcher.appendTail(result);

        return result.toString();
    }
}
