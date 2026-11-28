package com.prometis.appbuilder.console.app.deploy.dsl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.prometis.appbuilder.console.app.deploy.dto.DeployImportChange;

import java.util.*;

/**
 * 저장소 DSL 과 이 환경의 DSL 을 비교해 메타데이터(종류 + ID) 단위의 변경을 만든다. GitHub 와 DB 를 모르는 순수 계산이다.
 *
 * 경로는 모두 애플리케이션 폴더 아래 경로(workflow/query/Q001/query.sql)다. 저장소는 sourceApplicationId 폴더,
 * 이 환경은 지금 애플리케이션 폴더 기준이라 두 애플리케이션 ID 가 달라도 같은 경로끼리 비교한다.
 */
public final class DslImportPlanner {

    private DslImportPlanner() {
    }

    /**
     * changes 와 previewChanges 는 같은 순서로 하나씩 짝이 맞는다.
     */
    public record Plan(List<DslImporter.Change> changes, List<DeployImportChange> previewChanges) {

        /**
         * 고른 메타데이터(keyOf)만 남긴다. keys 가 null 이면 전부.
         */
        public Plan select(Collection<String> keys) {
            if(keys == null) {
                return this;
            }
            Set<String> selected = new HashSet<>(keys);
            List<DslImporter.Change> selectedChanges = new ArrayList<>();
            List<DeployImportChange> selectedPreview = new ArrayList<>();
            for(int i = 0; i < changes.size(); i++) {
                DeployImportChange preview = previewChanges.get(i);
                if(selected.contains(keyOf(preview.kind(), preview.id()))) {
                    selectedChanges.add(changes.get(i));
                    selectedPreview.add(preview);
                }
            }
            return new Plan(selectedChanges, selectedPreview);
        }

        /**
         * 이 계획만 반영했을 때의 DSL (경로 -> 내용). 이 환경의 DSL 에 고른 메타데이터의 파일만 저장소 내용으로 바꾼 것이다.
         * 일부만 골라 배포할 때 참조가 깨지지 않는지 DslValidator 로 미리 보는 데 쓴다.
         */
        public Map<String, String> resultFiles(Map<String, DslFile> localFiles, Map<String, String> remoteContents) {
            Map<String, String> result = new TreeMap<>();
            localFiles.forEach((path, file) -> result.put(path, file.content()));
            for(DeployImportChange change : previewChanges) {
                for(DeployImportChange.FileChange file : change.files()) {
                    if("DELETED".equals(file.status())) {
                        result.remove(file.path());
                    }
                    else {
                        result.put(file.path(), remoteContents.get(file.path()));
                    }
                }
            }
            return result;
        }
    }

    /**
     * 화면에서 고른 메타데이터를 가리키는 키. ID 에는 / 가 들어갈 수 없다(DslValidator).
     */
    public static String keyOf(String kind, String id) {
        return kind + "/" + id;
    }

    private record GroupKey(DslFolder folder, String id) {
    }

    private static final Comparator<GroupKey> ORDER = Comparator.comparing((GroupKey key) -> key.folder().ordinal()).thenComparing(GroupKey::id);

    /**
     * @param source         저장소에서 읽은 애플리케이션 ID (경로 해석에만 쓴다)
     * @param localFiles     이 환경의 DSL. 경로 -> 파일
     * @param remoteContents 저장소의 DSL. 경로 -> 내용
     * @param withContent    미리보기에 보여줄 파일 내용(before/after)을 담을지
     */
    public static Plan plan(String source, Map<String, DslFile> localFiles, Map<String, String> remoteContents, boolean withContent) {
        String prefix = source + "/";

        Map<GroupKey, Set<String>> localGroups = new TreeMap<>(ORDER);
        Map<GroupKey, Set<String>> remoteGroups = new TreeMap<>(ORDER);
        localFiles.forEach((path, file) -> localGroups.computeIfAbsent(new GroupKey(file.folder(), file.id()), k -> new TreeSet<>()).add(path));
        remoteContents.keySet().forEach(path -> DslFolder.locate(source, prefix + path).ifPresent(location ->
                remoteGroups.computeIfAbsent(new GroupKey(location.folder(), location.id()), k -> new TreeSet<>()).add(path)));

        Set<GroupKey> keys = new TreeSet<>(ORDER);
        keys.addAll(localGroups.keySet());
        keys.addAll(remoteGroups.keySet());

        List<DslImporter.Change> changes = new ArrayList<>();
        List<DeployImportChange> previewChanges = new ArrayList<>();

        for(GroupKey key : keys) {
            Set<String> localPaths = localGroups.getOrDefault(key, Set.of());
            Set<String> remotePaths = remoteGroups.getOrDefault(key, Set.of());

            List<DeployImportChange.FileChange> fileChanges = new ArrayList<>();
            Set<String> allPaths = new TreeSet<>(localPaths);
            allPaths.addAll(remotePaths);
            for(String path : allPaths) {
                DslFile local = localPaths.contains(path) ? localFiles.get(path) : null;
                String remote = remotePaths.contains(path) ? remoteContents.get(path) : null;

                if(local == null) {
                    fileChanges.add(new DeployImportChange.FileChange(path, "ADDED", null, withContent ? remote : null));
                }
                else if(remote == null) {
                    fileChanges.add(new DeployImportChange.FileChange(path, "DELETED", withContent ? local.content() : null, null));
                }
                else if(!remote.equals(local.content())) {
                    fileChanges.add(new DeployImportChange.FileChange(path, "MODIFIED",
                            withContent ? local.content() : null, withContent ? remote : null));
                }
            }
            if(fileChanges.isEmpty()) {
                continue;
            }

            DslImporter.Action action = remotePaths.isEmpty() ? DslImporter.Action.DELETE
                    : localPaths.isEmpty() ? DslImporter.Action.CREATE
                    : DslImporter.Action.UPDATE;

            DslImporter.DslDocument document = null;
            if(action != DslImporter.Action.DELETE) {
                document = documentOf(key, source, remotePaths, remoteContents);
                if(document == null) {
                    // metadata.json 이 없거나 읽지 못했다. DslValidator 가 오류로 잡으므로 가져오기는 막힌다
                    continue;
                }
            }

            changes.add(new DslImporter.Change(key.folder(), key.id(), action, document));
            previewChanges.add(new DeployImportChange(key.folder().kind(), key.id(), action.name(), fileChanges));
        }

        return new Plan(changes, previewChanges);
    }

    // 메타데이터 하나의 파일들을 DslDocument 로 모은다. metadata.json 을 읽지 못하면 null
    private static DslImporter.DslDocument documentOf(GroupKey key, String source, Set<String> paths, Map<String, String> contents) {
        JsonNode metadata = null;
        String body = null;
        String page = null;
        Map<String, JsonNode> actionMetadata = new TreeMap<>();
        Map<String, String> actionScripts = new HashMap<>();

        for(String path : paths) {
            DslFolder.Location location = DslFolder.locate(source, source + "/" + path).orElse(null);
            if(location == null) {
                continue;
            }
            String content = contents.get(path);
            switch (location.part()) {
                case METADATA -> metadata = readJson(content);
                case SQL, SCRIPT -> body = content;
                case PAGE -> page = content;
                case ACTION -> {
                    JsonNode node = readJson(content);
                    if(node != null && node.isObject()) {
                        actionMetadata.put(location.partName(), node);
                    }
                }
                case ACTION_SCRIPT -> actionScripts.put(location.partName(), content);
            }
        }

        if(metadata == null || !metadata.isObject()) {
            return null;
        }

        List<DslImporter.ActionDocument> actions = new ArrayList<>();
        actionMetadata.forEach((actionName, node) -> actions.add(new DslImporter.ActionDocument(actionName, node, actionScripts.get(actionName))));

        return new DslImporter.DslDocument(key.folder(), key.id(), metadata, body, page, actions);
    }

    private static JsonNode readJson(String content) {
        try {
            return content == null ? null : DslJson.read(content);
        }
        catch (JsonProcessingException e) {
            return null;
        }
    }
}
