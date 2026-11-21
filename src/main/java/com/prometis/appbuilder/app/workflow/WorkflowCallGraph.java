package com.prometis.appbuilder.app.workflow;

import java.util.*;

/**
 * 워크플로우 실행(WORKFLOW) 노드로 이어지는 워크플로우 사이의 호출관계.
 * 워크플로우코드 -> 그 워크플로우가 실행하는 워크플로우코드들. 순환 호출(A -> B -> A, 자기 자신 호출 포함)을 찾는 데 쓴다.
 * 콘솔 저장, 배포 DSL 검증이 같은 판정을 쓰도록 한 곳에 둔다.
 */
public final class WorkflowCallGraph {
    private final Map<String, Set<String>> calls = new LinkedHashMap<>();

    public void add(String caller, String callee) {
        if(caller == null || callee == null || callee.isBlank()) {
            return;
        }
        calls.computeIfAbsent(caller, key -> new LinkedHashSet<>()).add(callee);
    }

    /**
     * 워크플로우가 실행하는 워크플로우 목록을 통째로 바꾼다 (저장 전에 새 노드 구성으로 판정할 때).
     */
    public void replace(String caller, Collection<String> callees) {
        calls.remove(caller);
        for(String callee : callees) {
            add(caller, callee);
        }
    }

    public Set<String> calleesOf(String caller) {
        return calls.getOrDefault(caller, Set.of());
    }

    /**
     * start 에서 출발해 다시 start 로 돌아오는 호출 경로. 없으면 빈 목록.
     * 경로는 start 로 시작해서 start 로 끝난다 (예: [A, B, A]).
     */
    public List<String> findCycle(String start) {
        Deque<String> path = new ArrayDeque<>();
        path.addLast(start);
        List<String> found = dfs(start, start, path, new HashSet<>());
        return found == null ? List.of() : found;
    }

    private List<String> dfs(String start, String current, Deque<String> path, Set<String> visited) {
        for(String next : calleesOf(current)) {
            if(next.equals(start)) {
                List<String> cycle = new ArrayList<>(path);
                cycle.add(start);
                return cycle;
            }
            if(!visited.add(next)) {
                continue;
            }

            path.addLast(next);
            List<String> found = dfs(start, next, path, visited);
            if(found != null) {
                return found;
            }
            path.removeLast();
        }
        return null;
    }

    /**
     * target 을 (직접 또는 거쳐서) 실행하는 워크플로우들. target 자신은 들어가지 않는다.
     * 이 워크플로우들을 target 에서 실행하면 순환이 된다.
     */
    public Set<String> callersOf(String target) {
        Map<String, Set<String>> reverse = new HashMap<>();
        calls.forEach((caller, callees) -> callees.forEach(callee ->
                reverse.computeIfAbsent(callee, key -> new LinkedHashSet<>()).add(caller)));

        Set<String> result = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(List.of(target));
        while(!queue.isEmpty()) {
            for(String caller : reverse.getOrDefault(queue.poll(), Set.of())) {
                if(!caller.equals(target) && result.add(caller)) {
                    queue.add(caller);
                }
            }
        }
        return result;
    }

    public static String format(List<String> cycle) {
        return String.join(" → ", cycle);
    }
}
