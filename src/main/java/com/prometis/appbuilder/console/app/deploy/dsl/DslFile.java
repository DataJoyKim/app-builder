package com.prometis.appbuilder.console.app.deploy.dsl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * DSL 파일 하나. path 는 저장소 최상위 기준 경로({applicationId}/workflow/workflow/W001.json)이고 content 는 JSON 본문이다.
 * 쿼리의 SQL 본문 파일({applicationId}/workflow/query/Q001/query.sql)도 같은 folder/id 로 하나의 DslFile 이 되며, content 는 SQL 그대로다.
 */
public record DslFile(String path, DslFolder folder, String id, String content) {

    /**
     * 이 내용을 git 에 올렸을 때의 blob SHA. 저장소 트리의 SHA 와 같으면 내용이 같으므로 파일을 내려받지 않고도 비교할 수 있다.
     */
    public String gitBlobSha() {
        return gitBlobSha(content);
    }

    public static String gitBlobSha(String content) {
        byte[] body = content.getBytes(StandardCharsets.UTF_8);
        byte[] header = ("blob " + body.length + "\0").getBytes(StandardCharsets.UTF_8);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(header);
            digest.update(body);
            return HexFormat.of().formatHex(digest.digest());
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
