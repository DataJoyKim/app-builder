package com.prometis.appbuilder.console.app.deploy;

import com.prometis.appbuilder.console.app.deploy.dsl.DslExporter;
import com.prometis.appbuilder.console.app.deploy.dsl.DslFile;
import com.prometis.appbuilder.console.app.deploy.dsl.DslFolder;
import com.prometis.appbuilder.console.app.deploy.dsl.DslJson;
import com.prometis.appbuilder.console.app.deploy.dsl.DslValidator;
import com.prometis.appbuilder.console.app.deploy.dto.*;
import com.prometis.appbuilder.console.app.deploy.github.GitHubClient;
import com.prometis.appbuilder.console.app.deploy.github.GitHubException;
import com.prometis.appbuilder.console.app.deploy.github.GitHubTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 저장소 비교(추가/수정/삭제 판정)와 커밋(검증, 동시 커밋 감지, 바뀐 파일만 올리기)을 확인한다.
 */
class DeployServiceTest {
    private static final String APP = "ehr";
    private static final String TOKEN = "ghp_test";
    private static final String HEAD = "head-sha";

    private DeployGitSettingRepository settingRepository;
    private DeployTokenCipher tokenCipher;
    private GitHubClient gitHubClient;
    private DslExporter dslExporter;
    private DeployService deployService;

    private DslFile same;
    private DslFile changed;
    private DslFile added;

    @BeforeEach
    void setUp() {
        DeployProperties properties = new DeployProperties(null, "test-key", Duration.ofSeconds(1), null);
        settingRepository = mock(DeployGitSettingRepository.class);
        tokenCipher = new DeployTokenCipher(properties);
        gitHubClient = mock(GitHubClient.class);
        dslExporter = mock(DslExporter.class);

        deployService = new DeployService(settingRepository, tokenCipher, properties, gitHubClient, dslExporter, new DslValidator());

        when(settingRepository.findByApplicationId(APP)).thenReturn(Optional.of(DeployGitSetting.builder()
                .applicationId(APP).owner("acme").repository("dsl").branch("main")
                .encryptedToken(tokenCipher.encrypt(TOKEN)).repositoryUrl("https://github.com/acme/dsl")
                .build()));

        same = codeKind("C001", "본사");
        changed = codeKind("C002", "지사(변경)");
        added = codeKind("C003", "신규");
        when(dslExporter.export(APP)).thenReturn(List.of(same, changed, added));

        when(gitHubClient.findBranchHead(any(), eq("main"))).thenReturn(Optional.of(HEAD));
        when(gitHubClient.getCommitTreeSha(any(), eq(HEAD))).thenReturn("tree-sha");
        when(gitHubClient.getTreeFiles(any(), eq("tree-sha"))).thenReturn(List.of(
                new GitHubClient.TreeEntry(same.path(), same.gitBlobSha()),
                new GitHubClient.TreeEntry(changed.path(), "old-sha"),
                new GitHubClient.TreeEntry(DslFolder.COMMON_CODE.filePath(APP, "C999"), "removed-sha"),
                // 다른 애플리케이션과 json 이 아닌 파일은 건드리지 않는다
                new GitHubClient.TreeEntry("other/setting/common-code/C001.json", "x"),
                new GitHubClient.TreeEntry(APP + "/README.md", "y"),
                new GitHubClient.TreeEntry("README.md", "z")
        ));
        when(gitHubClient.getBlobText(any(), anyString())).thenAnswer(invocation -> "remote:" + invocation.getArgument(1));
    }

    @Test
    void 저장소와_비교해_추가_수정_삭제를_찾는다() {
        DeployCompareResult result = deployService.compare(APP);

        assertEquals(HEAD, result.baseCommitSha());
        assertEquals(3, result.fileCount());
        assertTrue(result.committable());

        Map<String, DeployChange.Status> statuses = new HashMap<>();
        result.changes().forEach(change -> statuses.put(change.id(), change.status()));
        assertEquals(Map.of(
                "C002", DeployChange.Status.MODIFIED,
                "C003", DeployChange.Status.ADDED,
                "C999", DeployChange.Status.DELETED
        ), statuses);

        DeployChange modified = result.changes().stream().filter(c -> c.id().equals("C002")).findFirst().orElseThrow();
        assertEquals("remote:old-sha", modified.before());
        assertEquals(changed.content(), modified.after());

        // 같은 파일은 SHA 로만 비교하고 내려받지 않는다
        verify(gitHubClient, never()).getBlobText(any(), eq(same.gitBlobSha()));
        // 토큰은 복호화되어 GitHub 로 간다
        verify(gitHubClient).findBranchHead(eq(new GitHubTarget("acme", "dsl", TOKEN)), eq("main"));
    }

    @Test
    void 바뀐_파일만_커밋하고_브랜치를_옮긴다() {
        when(gitHubClient.createTree(any(), eq("tree-sha"), anyList(), anyInt())).thenReturn("new-tree");
        when(gitHubClient.createCommit(any(), anyString(), eq("new-tree"), eq(HEAD))).thenReturn(new GitHubClient.CommitInfo("new-commit", "https://github.com/acme/dsl/commit/new-commit"));

        DeployCommitResult result = deployService.commit(APP, new DeployCommitRequest("회사 수정", HEAD, null), "홍길동");

        assertEquals("new-commit", result.commitSha());
        assertEquals(1, result.added());
        assertEquals(1, result.modified());
        assertEquals(1, result.deleted());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GitHubClient.FileChange>> changes = ArgumentCaptor.forClass(List.class);
        verify(gitHubClient).createTree(any(), eq("tree-sha"), changes.capture(), anyInt());
        Map<String, String> byPath = new HashMap<>();
        changes.getValue().forEach(change -> byPath.put(change.path(), change.content()));
        assertEquals(3, byPath.size());
        assertEquals(added.content(), byPath.get(added.path()));
        assertNull(byPath.get(DslFolder.COMMON_CODE.filePath(APP, "C999")));
        assertTrue(byPath.containsKey(DslFolder.COMMON_CODE.filePath(APP, "C999")));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(gitHubClient).createCommit(any(), message.capture(), eq("new-tree"), eq(HEAD));
        assertTrue(message.getValue().startsWith("회사 수정\n\nApp-Builder-Application: ehr"));
        assertTrue(message.getValue().contains("App-Builder-User: 홍길동"));

        verify(gitHubClient).updateBranch(any(), eq("main"), eq("new-commit"));
        // 커밋할 때는 비교 화면용 내용을 내려받지 않는다
        verify(gitHubClient, never()).getBlobText(any(), anyString());
    }

    @Test
    void 고른_파일만_커밋한다() {
        when(gitHubClient.createTree(any(), eq("tree-sha"), anyList(), anyInt())).thenReturn("new-tree");
        when(gitHubClient.createCommit(any(), anyString(), eq("new-tree"), eq(HEAD))).thenReturn(new GitHubClient.CommitInfo("new-commit", null));

        // 같은 파일(same)은 골라도 달라진 게 없으니 빠진다
        DeployCommitResult result = deployService.commit(APP,
                new DeployCommitRequest("추가만", HEAD, List.of(added.path(), same.path())), null);

        assertEquals(1, result.added());
        assertEquals(0, result.modified());
        assertEquals(0, result.deleted());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GitHubClient.FileChange>> changes = ArgumentCaptor.forClass(List.class);
        verify(gitHubClient).createTree(any(), eq("tree-sha"), changes.capture(), anyInt());
        assertEquals(List.of(added.path()), changes.getValue().stream().map(GitHubClient.FileChange::path).toList());
    }

    @Test
    void 고른_파일이_모두_달라지지_않았으면_커밋하지_않는다() {
        DeployException e = assertThrows(DeployException.class,
                () -> deployService.commit(APP, new DeployCommitRequest("없음", HEAD, List.of(same.path(), "ehr/nothing.json")), null));

        assertEquals(400, e.getStatus());
        verify(gitHubClient, never()).createTree(any(), any(), anyList(), anyInt());
    }

    @Test
    void 메타데이터_하나에_딸린_파일은_하나만_골라도_함께_커밋한다() {
        String sql = DslFolder.QUERY.sqlFilePath(APP, "Q001");
        String metadata = DslFolder.QUERY.filePath(APP, "Q001");
        String oldMetadata = APP + "/workflow/query/Q001.json";
        List<DeployChange> changes = List.of(
                new DeployChange(metadata, "Query", "Q001", DeployChange.Status.ADDED, null, "{}"),
                new DeployChange(sql, "Query", "Q001", DeployChange.Status.ADDED, null, "select 1"),
                // 예전 위치 파일도 kind/ID 가 같으면 같은 묶음이다
                new DeployChange(oldMetadata, "Query", "Q001", DeployChange.Status.DELETED, "{}", null),
                new DeployChange(DslFolder.QUERY.sqlFilePath(APP, "Q002"), "Query", "Q002", DeployChange.Status.MODIFIED, "a", "b"),
                // 종류를 모르는 파일은 그 파일만
                new DeployChange(APP + "/unknown/X.json", null, "X", DeployChange.Status.DELETED, "{}", null),
                new DeployChange(APP + "/unknown/Y.json", null, "Y", DeployChange.Status.DELETED, "{}", null));

        assertEquals(List.of(metadata, sql, oldMetadata),
                DeployService.selectChanges(changes, List.of(sql)).stream().map(DeployChange::path).toList());
        assertEquals(List.of(APP + "/unknown/X.json"),
                DeployService.selectChanges(changes, List.of(APP + "/unknown/X.json")).stream().map(DeployChange::path).toList());
        assertEquals(changes, DeployService.selectChanges(changes, null));
        assertEquals(List.of(), DeployService.selectChanges(changes, List.of()));
    }

    @Test
    void 비교한_뒤_브랜치가_바뀌었으면_커밋하지_않는다() {
        DeployException e = assertThrows(DeployException.class,
                () -> deployService.commit(APP, new DeployCommitRequest("회사 수정", "older-sha", null), null));

        assertEquals(409, e.getStatus());
        verify(gitHubClient, never()).createTree(any(), any(), anyList(), anyInt());
    }

    @Test
    void 커밋하는_사이_브랜치가_바뀌면_충돌로_알린다() {
        when(gitHubClient.createTree(any(), any(), anyList(), anyInt())).thenReturn("new-tree");
        when(gitHubClient.createCommit(any(), anyString(), any(), any())).thenReturn(new GitHubClient.CommitInfo("new-commit", null));
        doThrow(new GitHubException(422, "Update is not a fast forward")).when(gitHubClient).updateBranch(any(), any(), any());

        DeployException e = assertThrows(DeployException.class,
                () -> deployService.commit(APP, new DeployCommitRequest("회사 수정", HEAD, null), null));

        assertEquals(409, e.getStatus());
    }

    @Test
    void 검증_오류가_있으면_커밋하지_않는다() {
        Map<String, Object> query = DslJson.document(DslFolder.QUERY);
        query.put("queryName", "Q001");
        query.put("displayName", "조회");
        query.put("dataSourceName", "NO_DB");
        when(dslExporter.export(APP)).thenReturn(List.of(new DslFile(DslFolder.QUERY.filePath(APP, "Q001"), DslFolder.QUERY, "Q001", DslJson.write(query))));

        DeployException e = assertThrows(DeployException.class,
                () -> deployService.commit(APP, new DeployCommitRequest("쿼리", HEAD, null), null));

        assertEquals(400, e.getStatus());
        verify(gitHubClient, never()).findBranchHead(any(), any());
    }

    @Test
    void 변경이_없으면_커밋하지_않는다() {
        when(dslExporter.export(APP)).thenReturn(List.of(same));
        when(gitHubClient.getTreeFiles(any(), eq("tree-sha"))).thenReturn(List.of(new GitHubClient.TreeEntry(same.path(), same.gitBlobSha())));

        assertFalse(deployService.compare(APP).committable());
        assertThrows(DeployException.class, () -> deployService.commit(APP, new DeployCommitRequest("없음", HEAD, null), null));
    }

    @Test
    void 브랜치가_없으면_기본_브랜치에서_만든다() {
        when(gitHubClient.findBranchHead(any(), eq("main"))).thenReturn(Optional.empty());
        when(gitHubClient.getRepository(any())).thenReturn(new GitHubClient.RepositoryInfo("acme/dsl", "https://github.com/acme/dsl", "develop", true, false));
        when(gitHubClient.findBranchHead(any(), eq("develop"))).thenReturn(Optional.of(HEAD));
        when(gitHubClient.createTree(any(), any(), anyList(), anyInt())).thenReturn("new-tree");
        when(gitHubClient.createCommit(any(), anyString(), any(), any())).thenReturn(new GitHubClient.CommitInfo("new-commit", null));

        deployService.commit(APP, new DeployCommitRequest("처음", null, null), null);

        verify(gitHubClient).createBranch(any(), eq("main"), eq(HEAD));
        verify(gitHubClient).updateBranch(any(), eq("main"), eq("new-commit"));
    }

    @Test
    void 저장소_URL을_붙여넣어도_소유자와_저장소로_나눠_저장한다() {
        when(settingRepository.findByApplicationId("new")).thenReturn(Optional.empty());
        when(settingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(gitHubClient.getLogin(TOKEN)).thenReturn("octocat");
        when(gitHubClient.getRepository(new GitHubTarget("acme", "dsl-repo", TOKEN)))
                .thenReturn(new GitHubClient.RepositoryInfo("acme/dsl-repo", "https://github.com/acme/dsl-repo", "main", true, false));

        DeployGitSettingResponse response = deployService.saveSetting("new",
                new DeployGitSettingRequest("", "https://github.com/acme/dsl-repo.git", "", TOKEN));

        assertEquals("acme", response.owner());
        assertEquals("dsl-repo", response.repository());
        assertEquals("main", response.branch());
        assertEquals("octocat", response.githubLogin());

        ArgumentCaptor<DeployGitSetting> saved = ArgumentCaptor.forClass(DeployGitSetting.class);
        verify(settingRepository).save(saved.capture());
        assertNotEquals(TOKEN, saved.getValue().getEncryptedToken());
        assertEquals(TOKEN, tokenCipher.decrypt(saved.getValue().getEncryptedToken()));
    }

    @Test
    void 쓰기_권한이_없는_토큰은_저장하지_않는다() {
        when(gitHubClient.getLogin(TOKEN)).thenReturn("viewer");
        when(gitHubClient.getRepository(any())).thenReturn(new GitHubClient.RepositoryInfo("acme/dsl", "u", "main", false, false));

        assertThrows(DeployException.class, () -> deployService.saveSetting(APP, new DeployGitSettingRequest("acme", "dsl", "main", TOKEN)));
        verify(settingRepository, never()).save(any());
    }

    private static DslFile codeKind(String code, String name) {
        Map<String, Object> document = DslJson.document(DslFolder.COMMON_CODE);
        document.put("code", code);
        document.put("name", name);
        return new DslFile(DslFolder.COMMON_CODE.filePath(APP, code), DslFolder.COMMON_CODE, code, DslJson.write(document));
    }
}
