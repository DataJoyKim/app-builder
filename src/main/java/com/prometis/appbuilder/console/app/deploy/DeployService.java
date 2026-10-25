package com.prometis.appbuilder.console.app.deploy;

import com.prometis.appbuilder.console.app.deploy.dsl.DslExporter;
import com.prometis.appbuilder.console.app.deploy.dsl.DslFile;
import com.prometis.appbuilder.console.app.deploy.dsl.DslFolder;
import com.prometis.appbuilder.console.app.deploy.dsl.DslIssue;
import com.prometis.appbuilder.console.app.deploy.dsl.DslValidator;
import com.prometis.appbuilder.console.app.deploy.dto.*;
import com.prometis.appbuilder.console.app.deploy.github.GitHubClient;
import com.prometis.appbuilder.console.app.deploy.github.GitHubException;
import com.prometis.appbuilder.console.app.deploy.github.GitHubTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 배포 1단계: 메타데이터 -> DSL -> GitHub 커밋.
 *
 * 1. 연동 : 애플리케이션마다 GitHub 저장소/브랜치/토큰을 저장한다. 저장할 때 토큰으로 저장소에 쓸 수 있는지 확인한다.
 * 2. 비교 : 지금 메타데이터로 DSL 을 만들고 검증한 뒤, 브랜치의 {applicationId}/ 폴더와 비교해 추가/수정/삭제된 파일을 찾는다.
 *          내용 비교는 git blob SHA 로 하므로 바뀐 파일만 내려받는다.
 * 3. 커밋 : 다시 DSL 을 만들고 검증해서(오류가 있으면 멈춤) 바뀐 파일로 커밋을 만들고 브랜치를 옮긴다(push).
 *          비교한 뒤 브랜치에 다른 커밋이 올라왔으면 덮어쓰지 않고 다시 비교하게 한다.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(DeployProperties.class)
public class DeployService {
    private static final Pattern OWNER_OR_REPOSITORY = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Pattern REPOSITORY_URL = Pattern.compile("^(?:https?://[^/]+/|git@[^:]+:)?([^/\\s]+)/([^/\\s]+?)(?:\\.git)?/?$");
    private static final Pattern BRANCH = Pattern.compile("[A-Za-z0-9._/-]+");
    private static final String FIRST_COMMIT_FILE = "README.md";

    private final DeployGitSettingRepository settingRepository;
    private final DeployTokenCipher tokenCipher;
    private final DeployProperties properties;
    private final GitHubClient gitHubClient;
    private final DslExporter dslExporter;
    private final DslValidator dslValidator;

    // ---- 연동 설정 ----

    public DeployGitSettingResponse getSetting(String applicationId) {
        return settingRepository.findByApplicationId(applicationId)
                .map(DeployGitSettingResponse::of)
                .orElseGet(DeployGitSettingResponse::notConfigured);
    }

    /**
     * 연동 설정을 저장한다. 토큰으로 GitHub 계정과 저장소 쓰기 권한을 확인한 뒤에만 저장한다.
     */
    @Transactional
    public DeployGitSettingResponse saveSetting(String applicationId, DeployGitSettingRequest request) {
        String owner = trim(request.owner());
        String repository = trim(request.repository());

        // 저장소 칸에 owner/repository 나 저장소 URL 을 붙여넣은 경우
        if(repository != null) {
            Matcher matcher = REPOSITORY_URL.matcher(repository);
            if(matcher.matches()) {
                owner = matcher.group(1);
                repository = matcher.group(2);
            }
        }

        if(owner == null || !OWNER_OR_REPOSITORY.matcher(owner).matches()) {
            throw DeployException.badRequest("저장소 소유자(owner)를 확인해주세요.");
        }
        if(repository == null || !OWNER_OR_REPOSITORY.matcher(repository).matches()) {
            throw DeployException.badRequest("저장소 이름을 확인해주세요.");
        }

        Optional<DeployGitSetting> saved = settingRepository.findByApplicationId(applicationId);

        String token = trim(request.token());
        if(token == null) {
            token = saved.map(setting -> tokenCipher.decrypt(setting.getEncryptedToken()))
                    .orElseThrow(() -> DeployException.badRequest("GitHub 토큰을 입력해주세요."));
        }

        GitHubTarget target = new GitHubTarget(owner, repository, token);
        String login = gitHubClient.getLogin(token);
        GitHubClient.RepositoryInfo repositoryInfo = gitHubClient.getRepository(target);
        if(!repositoryInfo.canPush()) {
            throw DeployException.badRequest("토큰 계정(" + login + ")에 저장소 쓰기(push) 권한이 없습니다.");
        }

        String branch = trim(request.branch());
        if(branch == null) {
            branch = repositoryInfo.defaultBranch() == null ? "main" : repositoryInfo.defaultBranch();
        }
        checkBranchName(branch);

        String encryptedToken = tokenCipher.encrypt(token);
        DeployGitSetting setting = saved.orElseGet(() -> DeployGitSetting.builder().applicationId(applicationId).build());
        setting.update(owner, repository, branch, encryptedToken, login, repositoryInfo.htmlUrl());

        return DeployGitSettingResponse.of(settingRepository.save(setting));
    }

    @Transactional
    public void deleteSetting(String applicationId) {
        settingRepository.findByApplicationId(applicationId).ifPresent(settingRepository::delete);
    }

    // ---- DSL ----

    public DslSnapshotResponse getDsl(String applicationId) {
        List<DslFile> files = dslExporter.export(applicationId);
        List<String> folders = Arrays.stream(DslFolder.values()).map(DslFolder::path).toList();
        return new DslSnapshotResponse(folders, files, dslValidator.validate(applicationId, contentsOf(files)));
    }

    // ---- 비교 / 커밋 ----

    public DeployCompareResult compare(String applicationId) {
        DeployGitSetting setting = requireSetting(applicationId);
        GitHubTarget target = targetOf(setting);

        List<DslFile> files = dslExporter.export(applicationId);
        List<DslIssue> issues = dslValidator.validate(applicationId, contentsOf(files));

        String headSha = gitHubClient.findBranchHead(target, setting.getBranch()).orElse(null);
        List<DeployChange> changes = diff(target, applicationId, files, headSha, true);

        return new DeployCompareResult(
                setting.getBranch(),
                setting.getRepositoryUrl(),
                headSha,
                files.size(),
                changes,
                issues,
                !changes.isEmpty() && !DslValidator.hasError(issues)
        );
    }

    public DeployCommitResult commit(String applicationId, DeployCommitRequest request, String userName) {
        String message = trim(request.message());
        if(message == null) {
            throw DeployException.badRequest("커밋 메시지를 입력해주세요.");
        }

        DeployGitSetting setting = requireSetting(applicationId);
        GitHubTarget target = targetOf(setting);
        String branch = setting.getBranch();

        // 비교한 뒤 메타데이터가 바뀌었을 수 있으므로 커밋할 내용으로 다시 검증한다
        List<DslFile> files = dslExporter.export(applicationId);
        List<DslIssue> issues = dslValidator.validate(applicationId, contentsOf(files));
        long errorCount = issues.stream().filter(DslIssue::isError).count();
        if(errorCount > 0) {
            throw DeployException.badRequest("DSL 검증 오류가 " + errorCount + "건 있어 커밋할 수 없습니다. 비교를 다시 실행해 오류를 확인해주세요.");
        }

        String headSha = gitHubClient.findBranchHead(target, branch).orElse(null);
        if(!Objects.equals(headSha, trim(request.baseCommitSha()))) {
            throw DeployException.conflict("비교한 뒤 브랜치(" + branch + ")에 다른 커밋이 올라왔습니다. 다시 비교한 뒤 커밋해주세요.");
        }

        String parentSha = headSha == null ? createBranch(target, branch) : headSha;

        List<DeployChange> changes = diff(target, applicationId, files, parentSha, false);
        if(changes.isEmpty()) {
            throw DeployException.badRequest("저장소와 달라진 내용이 없습니다.");
        }

        List<GitHubClient.FileChange> fileChanges = changes.stream()
                .map(change -> new GitHubClient.FileChange(change.path(), change.after()))
                .toList();

        String baseTreeSha = gitHubClient.getCommitTreeSha(target, parentSha);
        String treeSha = gitHubClient.createTree(target, baseTreeSha, fileChanges, properties.inlineContentLimit());
        GitHubClient.CommitInfo commit = gitHubClient.createCommit(target, commitMessage(message, applicationId, userName), treeSha, parentSha);

        try {
            gitHubClient.updateBranch(target, branch, commit.sha());
        }
        catch (GitHubException e) {
            if(e.getStatus() == 422) {
                throw DeployException.conflict("커밋하는 사이 브랜치(" + branch + ")에 다른 커밋이 올라왔습니다. 다시 비교한 뒤 커밋해주세요.");
            }
            throw e;
        }

        return new DeployCommitResult(
                commit.sha(),
                commit.htmlUrl(),
                branch,
                count(changes, DeployChange.Status.ADDED),
                count(changes, DeployChange.Status.MODIFIED),
                count(changes, DeployChange.Status.DELETED)
        );
    }

    /**
     * 저장소의 {applicationId}/ 아래 .json/.sql 파일과 비교한다. 저장소에만 있는 파일은 메타데이터에서 지워진 것이므로 삭제로 본다.
     *
     * @param withContent 화면에 보여줄 비교 내용(before)까지 내려받을지. 커밋할 때는 필요 없다
     */
    private List<DeployChange> diff(GitHubTarget target, String applicationId, List<DslFile> files, String commitSha, boolean withContent) {
        Map<String, String> remoteShas = new TreeMap<>();
        if(commitSha != null) {
            String prefix = applicationId + "/";
            gitHubClient.getTreeFiles(target, gitHubClient.getCommitTreeSha(target, commitSha)).stream()
                    .filter(entry -> entry.path().startsWith(prefix) && DslFolder.isDslFile(entry.path()))
                    .forEach(entry -> remoteShas.put(entry.path(), entry.sha()));
        }

        List<DeployChange> changes = new ArrayList<>();
        Set<String> localPaths = new HashSet<>();

        for(DslFile file : files) {
            localPaths.add(file.path());
            String remoteSha = remoteShas.get(file.path());

            if(remoteSha == null) {
                changes.add(new DeployChange(file.path(), file.folder().kind(), file.id(), DeployChange.Status.ADDED, null, file.content()));
            }
            else if(!remoteSha.equals(file.gitBlobSha())) {
                String before = withContent ? gitHubClient.getBlobText(target, remoteSha) : null;
                changes.add(new DeployChange(file.path(), file.folder().kind(), file.id(), DeployChange.Status.MODIFIED, before, file.content()));
            }
        }

        remoteShas.forEach((path, sha) -> {
            if(!localPaths.contains(path)) {
                String before = withContent ? gitHubClient.getBlobText(target, sha) : null;
                changes.add(new DeployChange(path, kindOf(applicationId, path), idOf(applicationId, path), DeployChange.Status.DELETED, before, null));
            }
        });

        changes.sort(Comparator.comparing(DeployChange::path));
        return changes;
    }

    /**
     * 연동한 브랜치가 아직 없을 때 만든다.
     * 저장소가 비어있으면(커밋이 없으면) 기본 브랜치에 첫 커밋을 만들고, 그 커밋에서 브랜치를 딴다.
     * @return 새 브랜치의 최신 커밋
     */
    private String createBranch(GitHubTarget target, String branch) {
        GitHubClient.RepositoryInfo repositoryInfo = gitHubClient.getRepository(target);
        String defaultBranch = repositoryInfo.defaultBranch();

        Optional<String> defaultHead = defaultBranch == null ? Optional.empty() : gitHubClient.findBranchHead(target, defaultBranch);
        String baseSha = defaultHead.orElseGet(() -> gitHubClient.createFirstCommit(
                target,
                FIRST_COMMIT_FILE,
                "# App Builder DSL\n\nApp Builder 의 애플리케이션 메타데이터 DSL 저장소입니다. 애플리케이션마다 {applicationId}/ 폴더에 저장됩니다.\n",
                "Initialize App Builder DSL repository"));

        if(branch.equals(defaultBranch) && defaultHead.isEmpty()) {
            // 빈 저장소의 첫 커밋은 기본 브랜치에 만들어지므로 연동 브랜치가 기본 브랜치면 그대로 쓴다
            return baseSha;
        }

        gitHubClient.createBranch(target, branch, baseSha);
        return baseSha;
    }

    // ---- util ----

    private DeployGitSetting requireSetting(String applicationId) {
        return settingRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> DeployException.badRequest("GitHub 저장소 연동 설정을 먼저 저장해주세요."));
    }

    private GitHubTarget targetOf(DeployGitSetting setting) {
        return new GitHubTarget(setting.getOwner(), setting.getRepository(), tokenCipher.decrypt(setting.getEncryptedToken()));
    }

    private static Map<String, String> contentsOf(List<DslFile> files) {
        Map<String, String> contents = new LinkedHashMap<>();
        files.forEach(file -> contents.put(file.path(), file.content()));
        return contents;
    }

    private static String commitMessage(String message, String applicationId, String userName) {
        StringBuilder builder = new StringBuilder(message)
                .append("\n\nApp-Builder-Application: ").append(applicationId);
        if(userName != null && !userName.isBlank()) {
            builder.append("\nApp-Builder-User: ").append(userName);
        }
        return builder.toString();
    }

    private static void checkBranchName(String branch) {
        if(!BRANCH.matcher(branch).matches()
                || branch.contains("..") || branch.contains("//")
                || branch.startsWith("/") || branch.endsWith("/")
                || branch.startsWith("-") || branch.endsWith(".lock")) {
            throw DeployException.badRequest("브랜치 이름을 확인해주세요. [" + branch + "]");
        }
    }

    // 저장소에만 남은 파일의 종류. 지금 규칙의 위치가 아니면(예: 예전 workflow/query/Q001.json) 바로 위 폴더로 찾고, 그래도 모르면 null
    private static String kindOf(String applicationId, String path) {
        return DslFolder.locate(applicationId, path)
                .map(location -> location.folder().kind())
                .orElseGet(() -> {
                    String relative = path.substring(applicationId.length() + 1);
                    int slash = relative.lastIndexOf('/');
                    return DslFolder.ofPath(slash < 0 ? "" : relative.substring(0, slash)).map(DslFolder::kind).orElse(null);
                });
    }

    // 쿼리(workflow/query/{ID}/metadata.json)는 폴더명이 ID 다
    private static String idOf(String applicationId, String path) {
        Optional<DslFolder.Location> location = DslFolder.locate(applicationId, path);
        if(location.isPresent()) {
            return location.get().id();
        }

        String name = path.substring(path.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private static int count(List<DeployChange> changes, DeployChange.Status status) {
        return (int) changes.stream().filter(change -> status.equals(change.status())).count();
    }

    private static String trim(String value) {
        if(value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
