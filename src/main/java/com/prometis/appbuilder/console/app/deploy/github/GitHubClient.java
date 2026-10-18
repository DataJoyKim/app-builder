package com.prometis.appbuilder.console.app.deploy.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prometis.appbuilder.console.app.deploy.DeployProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * GitHub REST API 호출. 로컬에 git 을 두지 않고 Git Data API(blob/tree/commit/ref)로 바로 커밋하고 브랜치를 옮긴다(= push).
 * 인증은 Personal Access Token(Contents 읽기/쓰기 권한)을 Bearer 로 보낸다.
 */
@Component
public class GitHubClient {
    private static final String API_VERSION = "2022-11-28";
    private static final String FILE_MODE = "100644";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GitHubClient(DeployProperties properties) {
        // PATCH(브랜치 이동)를 쓰므로 HttpComponents 를 쓴다. JDK 기본 HttpURLConnection 은 PATCH 를 못 보낸다
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setConnectionRequestTimeout(properties.connectTimeout());

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.githubApiUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", API_VERSION)
                .build();
    }

    public record RepositoryInfo(String fullName, String htmlUrl, String defaultBranch, boolean canPush, boolean empty) {
    }

    public record TreeEntry(String path, String sha) {
    }

    /**
     * 트리에 넣을 파일 변경. content 가 null 이면 삭제다.
     */
    public record FileChange(String path, String content) {
        public boolean isDelete() {
            return content == null;
        }
    }

    public record CommitInfo(String sha, String htmlUrl) {
    }

    /**
     * 토큰 주인의 로그인 ID. 토큰이 잘못되면 401 로 실패한다.
     */
    public String getLogin(String token) {
        JsonNode user = call(() -> restClient.get()
                .uri(uri -> uri.pathSegment("user").build())
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(JsonNode.class));
        return user.path("login").asText(null);
    }

    public RepositoryInfo getRepository(GitHubTarget target) {
        JsonNode repo = call(() -> get(target, uri -> repoUri(uri, target).build()));
        return new RepositoryInfo(
                repo.path("full_name").asText(),
                repo.path("html_url").asText(),
                repo.path("default_branch").asText(null),
                repo.path("permissions").path("push").asBoolean(false),
                repo.path("size").asInt(0) == 0
        );
    }

    /**
     * 브랜치의 최신 커밋. 브랜치가 없거나 저장소가 비어있으면(커밋이 하나도 없으면) empty.
     */
    public Optional<String> findBranchHead(GitHubTarget target, String branch) {
        try {
            JsonNode ref = call(() -> get(target, uri -> repoUri(uri, target).pathSegment("git", "ref", "heads").pathSegment(segments(branch)).build()));
            return Optional.ofNullable(ref.path("object").path("sha").asText(null));
        }
        catch (GitHubException e) {
            // 404: 브랜치 없음, 409: 빈 저장소
            if(e.getStatus() == HttpStatus.NOT_FOUND.value() || e.getStatus() == HttpStatus.CONFLICT.value()) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public String getCommitTreeSha(GitHubTarget target, String commitSha) {
        JsonNode commit = call(() -> get(target, uri -> repoUri(uri, target).pathSegment("git", "commits", commitSha).build()));
        return commit.path("tree").path("sha").asText();
    }

    /**
     * 트리 전체(하위 폴더 포함)의 파일(blob) 목록.
     */
    public List<TreeEntry> getTreeFiles(GitHubTarget target, String treeSha) {
        JsonNode tree = call(() -> get(target, uri -> repoUri(uri, target).pathSegment("git", "trees", treeSha).queryParam("recursive", "1").build()));

        if(tree.path("truncated").asBoolean(false)) {
            throw new GitHubException(0, "저장소의 파일이 너무 많아 GitHub 가 목록을 잘라서 보냈습니다. 저장소를 나눠서 사용해주세요.");
        }

        List<TreeEntry> entries = new ArrayList<>();
        for(JsonNode entry : tree.path("tree")) {
            if("blob".equals(entry.path("type").asText())) {
                entries.add(new TreeEntry(entry.path("path").asText(), entry.path("sha").asText()));
            }
        }
        return entries;
    }

    public String getBlobText(GitHubTarget target, String blobSha) {
        JsonNode blob = call(() -> get(target, uri -> repoUri(uri, target).pathSegment("git", "blobs", blobSha).build()));

        String content = blob.path("content").asText("");
        if("base64".equals(blob.path("encoding").asText())) {
            return new String(Base64.getMimeDecoder().decode(content), StandardCharsets.UTF_8);
        }
        return content;
    }

    /**
     * baseTreeSha 위에 파일 변경을 얹은 새 트리. 바뀌지 않은 파일은 baseTree 에서 그대로 이어받는다.
     */
    public String createTree(GitHubTarget target, String baseTreeSha, List<FileChange> changes, int inlineContentLimit) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for(FileChange change : changes) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("path", change.path());
            entry.put("mode", FILE_MODE);
            entry.put("type", "blob");

            if(change.isDelete()) {
                entry.put("sha", null);
            }
            else if(change.content().length() > inlineContentLimit) {
                // 큰 파일(이미지가 담긴 DSL 등)은 blob 으로 먼저 올려 트리 요청 하나가 너무 커지지 않게 한다
                entry.put("sha", createBlob(target, change.content()));
            }
            else {
                entry.put("content", change.content());
            }
            entries.add(entry);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("base_tree", baseTreeSha);
        body.put("tree", entries);

        JsonNode tree = call(() -> post(target, uri -> repoUri(uri, target).pathSegment("git", "trees").build(), body));
        return tree.path("sha").asText();
    }

    public CommitInfo createCommit(GitHubTarget target, String message, String treeSha, String parentSha) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("tree", treeSha);
        body.put("parents", List.of(parentSha));

        JsonNode commit = call(() -> post(target, uri -> repoUri(uri, target).pathSegment("git", "commits").build(), body));
        return new CommitInfo(commit.path("sha").asText(), commit.path("html_url").asText(null));
    }

    /**
     * 브랜치를 commitSha 로 옮긴다(= push). force 를 쓰지 않으므로 그 사이 다른 커밋이 올라왔으면 422 로 실패한다.
     */
    public void updateBranch(GitHubTarget target, String branch, String commitSha) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sha", commitSha);
        body.put("force", false);

        call(() -> restClient.patch()
                .uri(uri -> repoUri(uri, target).pathSegment("git", "refs", "heads").pathSegment(segments(branch)).build())
                .headers(headers -> headers.setBearerAuth(target.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(json(body))
                .retrieve()
                .body(JsonNode.class));
    }

    public void createBranch(GitHubTarget target, String branch, String commitSha) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ref", "refs/heads/" + branch);
        body.put("sha", commitSha);

        call(() -> post(target, uri -> repoUri(uri, target).pathSegment("git", "refs").build(), body));
    }

    /**
     * 커밋이 하나도 없는 빈 저장소의 기본 브랜치에 첫 커밋을 만든다. Git Data API 는 빈 저장소에서 동작하지 않아 Contents API 로 파일 하나를 올린다.
     * @return 만들어진 커밋 SHA
     */
    public String createFirstCommit(GitHubTarget target, String path, String content, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));

        JsonNode result = call(() -> restClient.put()
                .uri(uri -> repoUri(uri, target).pathSegment("contents").pathSegment(segments(path)).build())
                .headers(headers -> headers.setBearerAuth(target.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(json(body))
                .retrieve()
                .body(JsonNode.class));
        return result.path("commit").path("sha").asText();
    }

    private String createBlob(GitHubTarget target, String content) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
        body.put("encoding", "base64");

        JsonNode blob = call(() -> post(target, uri -> repoUri(uri, target).pathSegment("git", "blobs").build(), body));
        return blob.path("sha").asText();
    }

    // ---- http ----

    private JsonNode get(GitHubTarget target, Function<UriBuilder, URI> uri) {
        return restClient.get()
                .uri(uri)
                .headers(headers -> headers.setBearerAuth(target.token()))
                .retrieve()
                .body(JsonNode.class);
    }

    private JsonNode post(GitHubTarget target, Function<UriBuilder, URI> uri, Object body) {
        return restClient.post()
                .uri(uri)
                .headers(headers -> headers.setBearerAuth(target.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(json(body))
                .retrieve()
                .body(JsonNode.class);
    }

    private static UriBuilder repoUri(UriBuilder uri, GitHubTarget target) {
        return uri.pathSegment("repos", target.owner(), target.repository());
    }

    // 브랜치(feature/x)나 파일 경로의 / 가 %2F 로 바뀌지 않게 조각마다 나눠 붙인다
    private static String[] segments(String path) {
        return path.split("/");
    }

    // null 값(삭제할 파일의 sha: null)도 그대로 보내야 하므로 직접 직렬화한다
    private String json(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        }
        catch (RestClientResponseException e) {
            throw new GitHubException(e.getStatusCode().value(), messageOf(e), e);
        }
        catch (ResourceAccessException e) {
            throw new GitHubException(0, "GitHub 에 연결할 수 없습니다. " + e.getMessage(), e);
        }
    }

    private static String messageOf(RestClientResponseException e) {
        String detail = null;
        try {
            JsonNode body = new ObjectMapper().readTree(e.getResponseBodyAsString());
            detail = body == null ? null : body.path("message").asText(null);
        }
        catch (Exception ignored) {
            // 본문이 JSON 이 아니면 상태코드만 쓴다
        }

        int status = e.getStatusCode().value();
        String reason = switch (status) {
            case 401 -> "GitHub 토큰이 올바르지 않거나 만료되었습니다.";
            case 403 -> "GitHub 토큰에 권한이 없습니다. (저장소 Contents 읽기/쓰기 권한이 필요합니다)";
            case 404 -> "저장소를 찾을 수 없거나 토큰으로 접근할 수 없습니다.";
            case 409 -> "저장소 상태가 요청과 맞지 않습니다.";
            case 422 -> "GitHub 가 요청을 처리할 수 없습니다.";
            default -> "GitHub 요청이 실패했습니다.";
        };

        return detail == null ? reason + " (HTTP " + status + ")" : reason + " (HTTP " + status + ": " + detail + ")";
    }
}
