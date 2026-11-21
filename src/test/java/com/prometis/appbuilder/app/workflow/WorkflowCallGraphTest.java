package com.prometis.appbuilder.app.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowCallGraphTest {
    @Test
    void 돌아오는_경로를_찾는다() {
        WorkflowCallGraph graph = new WorkflowCallGraph();
        graph.add("A", "B");
        graph.add("B", "C");
        graph.add("C", "A");
        graph.add("C", "D");

        assertEquals(List.of("A", "B", "C", "A"), graph.findCycle("A"));
        assertEquals(List.of(), graph.findCycle("D"));
    }

    @Test
    void 자기_자신_호출도_순환이다() {
        WorkflowCallGraph graph = new WorkflowCallGraph();
        graph.add("A", "A");

        assertEquals(List.of("A", "A"), graph.findCycle("A"));
    }

    @Test
    void 같은_워크플로우로_모이는_호출은_순환이_아니다() {
        WorkflowCallGraph graph = new WorkflowCallGraph();
        graph.add("A", "B");
        graph.add("A", "C");
        graph.add("B", "D");
        graph.add("C", "D");

        assertEquals(List.of(), graph.findCycle("A"));
    }

    @Test
    void 호출목록을_바꿔서_판정한다() {
        WorkflowCallGraph graph = new WorkflowCallGraph();
        graph.add("A", "B");
        graph.add("B", "C");

        graph.replace("C", List.of("A"));
        assertEquals(List.of("C", "A", "B", "C"), graph.findCycle("C"));

        graph.replace("C", List.of());
        assertEquals(List.of(), graph.findCycle("C"));
    }

    @Test
    void 거쳐서_실행하는_워크플로우까지_찾는다() {
        WorkflowCallGraph graph = new WorkflowCallGraph();
        graph.add("A", "B");
        graph.add("B", "C");
        graph.add("X", "Y");

        assertEquals(Set.of("A", "B"), graph.callersOf("C"));
        assertEquals(Set.of(), graph.callersOf("A"));
    }
}
