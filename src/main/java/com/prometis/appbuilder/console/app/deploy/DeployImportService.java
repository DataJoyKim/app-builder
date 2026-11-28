package com.prometis.appbuilder.console.app.deploy;

import com.prometis.appbuilder.app.scheduler.SchedulerManagementService;
import com.prometis.appbuilder.app.scheduler.domain.SchedulerJob;
import com.prometis.appbuilder.app.view.ViewSourceStorage;
import com.prometis.appbuilder.console.app.deploy.dsl.*;
import com.prometis.appbuilder.console.app.deploy.dto.*;
import com.prometis.appbuilder.console.app.deploy.github.GitHubClient;
import com.prometis.appbuilder.console.app.deploy.github.GitHubTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 배포 2단계: GitHub 저장소의 DSL -> 이 환경의 메타데이터 DB.
 *
 * 1. 미리보기 : 브랜치 최신 커밋의 {sourceApplicationId}/ DSL 을 지금 메타데이터로 만든 DSL 과 git blob SHA 로 비교해
 *              메타데이터(종류 + ID) 단위로 만들기/고치기/지우기를 보여준다. 저장소 DSL 전체를 DslValidator 로 검증하고
 *              이 환경에 반영할 수 없는 문제(DslImporter.check)도 함께 본다. 바뀐 파일만 내려받는다.
 * 2. 배포    : 미리보기한 커밋으로 다시 비교한 뒤, 고른 메타데이터(keys, 없으면 전부)만 반영했을 때의 DSL 을 검증하고(오류가 있으면 멈춤)
 *              DslImporter 가 한 트랜잭션으로 DB 에 반영한다.
 *              저장소 DSL 을 그대로 맞추는 것이라 저장소에 없는 메타데이터는 지운다.
 *              커밋 뒤에 FILE 화면 소스를 디스크에 쓰고 스케줄러를 다시 등록한다.
 *
 * sourceApplicationId 를 다른 애플리케이션으로 주면 그 애플리케이션 폴더의 DSL 을 지금 애플리케이션에 배포한다(예: 개발 -> 운영).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeployImportService {
    private static final Pattern APPLICATION_ID = Pattern.compile("[A-Za-z0-9_-]{1,100}");

    private final DeployGitSettingRepository settingRepository;
    private final DeployTokenCipher tokenCipher;
    private final GitHubClient gitHubClient;
    private final DslExporter dslExporter;
    private final DslValidator dslValidator;
    private final DslImporter dslImporter;
    private final ViewSourceStorage viewSourceStorage;
    private final SchedulerManagementService schedulerManagementService;

    public DeployImportPreview preview(String applicationId, DeployImportRequest request) {
        DeployGitSetting setting = requireSetting(applicationId);
        GitHubTarget target = targetOf(setting);
        String branch = branchOf(setting, request);
        String source = sourceOf(applicationId, request);

        String commitSha = gitHubClient.findBranchHead(target, branch)
                .orElseThrow(() -> DeployException.badRequest("저장소에 브랜치(" + branch + ")가 없습니다."));

        Plan plan = plan(target, applicationId, source, commitSha, true);
        return new DeployImportPreview(
                branch,
                setting.getRepositoryUrl(),
                commitSha,
                source,
                plan.sourceApplications,
                plan.remoteFileCount,
                plan.previewChanges,
                plan.issues,
                // 일부만 골라 배포할 수 있으므로 저장소 DSL 의 오류가 있어도 막지 않는다. 고른 것으로 배포할 때 다시 검증한다
                plan.remoteFileCount > 0 && !plan.changes.isEmpty()
        );
    }

    public DeployImportResult apply(String applicationId, DeployImportRequest request) {
        DeployGitSetting setting = requireSetting(applicationId);
        GitHubTarget target = targetOf(setting);
        String source = sourceOf(applicationId, request);

        String commitSha = trim(request.commitSha());
        if(commitSha == null || !commitSha.matches("[0-9a-fA-F]{7,64}")) {
            throw DeployException.badRequest("배포할 커밋이 없습니다. 미리보기를 먼저 실행해주세요.");
        }

        // 미리보기한 커밋을 기준으로 지금 메타데이터와 다시 비교한다 (그 사이 이 환경의 메타데이터가 바뀌었을 수 있다)
        Plan plan = plan(target, applicationId, source, commitSha, false);
        if(plan.remoteFileCount == 0) {
            throw DeployException.badRequest("저장소에 '" + source + "' 애플리케이션의 DSL 이 없습니다.");
        }
        if(plan.changes.isEmpty()) {
            throw DeployException.badRequest("저장소와 달라진 메타데이터가 없습니다.");
        }

        // 고른 메타데이터만. 미리보기 뒤에 더는 다르지 않게 된 것은 빠진다
        DslImportPlanner.Plan selected = plan.diff.select(request.keys());
        if(selected.changes().isEmpty()) {
            throw DeployException.badRequest("고른 메타데이터 중 저장소와 달라진 것이 없습니다. 미리보기를 다시 실행해주세요.");
        }

        // 고른 것만 반영했을 때의 DSL 을 검증한다. 일부만 고르면 참조가 깨질 수 있다 (예: 새 워크플로우가 고르지 않은 새 쿼리를 가리킴)
        Map<String, String> resultFiles = new LinkedHashMap<>();
        selected.resultFiles(plan.localFiles, plan.remoteContents)
                .forEach((path, content) -> resultFiles.put(applicationId + "/" + path, content));
        List<DslIssue> errors = new ArrayList<>();
        dslValidator.validate(applicationId, resultFiles).stream().filter(DslIssue::isError).forEach(errors::add);
        dslImporter.check(applicationId, selected.changes()).stream().filter(DslIssue::isError).forEach(errors::add);
        if(!errors.isEmpty()) {
            StringBuilder message = new StringBuilder("배포하면 오류가 " + errors.size() + "건 생겨 배포할 수 없습니다. 함께 배포해야 하는 메타데이터를 같이 골라주세요.");
            errors.stream().limit(5).forEach(issue -> message.append("\n- ")
                    .append(relativeOrSelf(applicationId, issue.path())).append(" : ").append(issue.message()));
            if(errors.size() > 5) {
                message.append("\n- ... 외 ").append(errors.size() - 5).append("건");
            }
            throw DeployException.badRequest(message.toString());
        }

        DslImporter.Outcome outcome = dslImporter.apply(applicationId, selected.changes());

        List<String> warnings = new ArrayList<>(outcome.postActions().warnings());
        runPostActions(applicationId, outcome.postActions(), warnings);

        return new DeployImportResult(commitSha, source, outcome.created(), outcome.updated(), outcome.deleted(), warnings);
    }

    // DB 커밋 뒤에 할 일. 실패해도 DB 는 되돌리지 않고 경고로 알린다
    private void runPostActions(String applicationId, DslImporter.PostActions actions, List<String> warnings) {
        actions.viewSourceWrites().forEach((objectCode, content) -> {
            try {
                viewSourceStorage.write(applicationId, objectCode, content);
            }
            catch (RuntimeException e) {
                warnings.add("화면 '" + objectCode + "' 의 HTML 소스를 쓰지 못했습니다. " + e.getMessage());
            }
        });

        actions.viewSourceDeletes().forEach(objectCode -> {
            try {
                viewSourceStorage.delete(applicationId, objectCode);
            }
            catch (RuntimeException e) {
                warnings.add("지운 화면 '" + objectCode + "' 의 HTML 소스를 지우지 못했습니다. " + e.getMessage());
            }
        });

        for(String jobCode : actions.unscheduleJobCodes()) {
            try {
                schedulerManagementService.unscheduleJob(jobCode);
            }
            catch (Exception e) {
                warnings.add("지운 스케줄러 작업 '" + jobCode + "' 를 내리지 못했습니다. " + e.getMessage());
            }
        }

        for(SchedulerJob job : actions.schedulerJobs()) {
            try {
                schedulerManagementService.scheduleOrReplace(job);
            }
            catch (Exception e) {
                warnings.add("스케줄러 작업 '" + job.getJobCode() + "' 를 등록하지 못했습니다. [스케줄러] 화면에서 다시 시작해주세요. " + e.getMessage());
            }
        }
    }

    // ================= 비교 =================

    private static class Plan {
        private Map<String, DslFile> localFiles = Map.of();
        private Map<String, String> remoteContents = Map.of();
        private DslImportPlanner.Plan diff = new DslImportPlanner.Plan(List.of(), List.of());
        private final List<String> sourceApplications = new ArrayList<>();
        private int remoteFileCount;
        private final List<DslImporter.Change> changes = new ArrayList<>();
        private final List<DeployImportChange> previewChanges = new ArrayList<>();
        private final List<DslIssue> issues = new ArrayList<>();
    }

    private Plan plan(GitHubTarget target, String applicationId, String source, String commitSha, boolean withContent) {
        Plan plan = new Plan();

        // 이 환경: 지금 메타데이터로 만든 DSL
        Map<String, DslFile> localFiles = new TreeMap<>();
        for(DslFile file : dslExporter.export(applicationId)) {
            localFiles.put(relative(applicationId, file.path()), file);
        }

        // 저장소: 커밋의 {source}/ 아래 DSL 파일 (경로 -> blob SHA)
        String prefix = source + "/";
        Map<String, String> remoteShas = new TreeMap<>();
        Set<String> sourceApplications = new TreeSet<>();
        for(GitHubClient.TreeEntry entry : gitHubClient.getTreeFiles(target, gitHubClient.getCommitTreeSha(target, commitSha))) {
            if(!DslFolder.isDslFile(entry.path())) {
                continue;
            }
            int slash = entry.path().indexOf('/');
            if(slash > 0) {
                sourceApplications.add(entry.path().substring(0, slash));
            }
            if(entry.path().startsWith(prefix)) {
                remoteShas.put(entry.path().substring(prefix.length()), entry.sha());
            }
        }
        plan.sourceApplications.addAll(sourceApplications);
        plan.remoteFileCount = remoteShas.size();
        if(remoteShas.isEmpty()) {
            plan.issues.add(DslIssue.error(prefix, "저장소(" + shortSha(commitSha) + ")에 '" + source + "' 애플리케이션의 DSL 이 없습니다."));
            return plan;
        }

        // 저장소 내용: SHA 가 같은 파일은 이 환경의 내용과 같으므로 내려받지 않는다
        Map<String, String> remoteContents = new TreeMap<>();
        remoteShas.forEach((path, sha) -> {
            DslFile local = localFiles.get(path);
            remoteContents.put(path, local != null && sha.equals(local.gitBlobSha())
                    ? local.content()
                    : gitHubClient.getBlobText(target, sha));
        });

        // 저장소 DSL 전체 검증 (파일 간 참조까지 보므로 바뀌지 않은 파일도 넣는다)
        Map<String, String> validateFiles = new LinkedHashMap<>();
        remoteContents.forEach((path, content) -> validateFiles.put(prefix + path, content));
        plan.issues.addAll(dslValidator.validate(source, validateFiles));

        DslImportPlanner.Plan diff = DslImportPlanner.plan(source, localFiles, remoteContents, withContent);
        plan.localFiles = localFiles;
        plan.remoteContents = remoteContents;
        plan.diff = diff;
        plan.changes.addAll(diff.changes());
        plan.previewChanges.addAll(diff.previewChanges());

        plan.issues.addAll(dslImporter.check(applicationId, plan.changes));
        return plan;
    }

    // ================= util =================

    private DeployGitSetting requireSetting(String applicationId) {
        return settingRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> DeployException.badRequest("GitHub 저장소 연동 설정을 먼저 저장해주세요. [Deploy > Repository]"));
    }

    private GitHubTarget targetOf(DeployGitSetting setting) {
        return new GitHubTarget(setting.getOwner(), setting.getRepository(), tokenCipher.decrypt(setting.getEncryptedToken()));
    }

    private static String branchOf(DeployGitSetting setting, DeployImportRequest request) {
        String branch = trim(request.branch());
        if(branch == null) {
            branch = setting.getBranch();
        }
        DeployService.checkBranchName(branch);
        return branch;
    }

    private static String sourceOf(String applicationId, DeployImportRequest request) {
        String source = trim(request.sourceApplicationId());
        if(source == null) {
            return applicationId;
        }
        if(!APPLICATION_ID.matcher(source).matches()) {
            throw DeployException.badRequest("배포할 저장소의 애플리케이션 폴더를 확인해주세요. [" + source + "]");
        }
        return source;
    }

    private static String relativeOrSelf(String applicationId, String path) {
        return path != null && path.startsWith(applicationId + "/") ? relative(applicationId, path) : path;
    }

    private static String relative(String applicationId, String path) {
        return path.substring(applicationId.length() + 1);
    }

    private static String shortSha(String sha) {
        return sha.length() > 7 ? sha.substring(0, 7) : sha;
    }

    private static String trim(String value) {
        if(value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
