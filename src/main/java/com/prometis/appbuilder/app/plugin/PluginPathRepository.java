package com.prometis.appbuilder.app.plugin;

import com.prometis.appbuilder.app.node.code.FunctionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PluginPathRepository extends JpaRepository<PluginPath, Long> {
    @Query("select p from PluginPath p join fetch p.plugin where p.applicationId = :applicationId and p.pluginPath = :pluginPath")
    Optional<PluginPath> findWithPlugin(@Param("applicationId") String applicationId, @Param("pluginPath") String pluginPath);

    @Query("select p from PluginPath p join fetch p.plugin pl where p.applicationId = :applicationId order by pl.pluginId, p.orderNum, p.pluginPath")
    List<PluginPath> findAllWithPlugin(@Param("applicationId") String applicationId);

    @Query("select p from PluginPath p where p.plugin.id = :pluginSeq order by p.orderNum, p.pluginPath")
    List<PluginPath> findByPluginSeq(@Param("pluginSeq") Long pluginSeq);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PluginPath p where p.plugin.id = :pluginSeq")
    int deleteByPluginSeq(@Param("pluginSeq") Long pluginSeq);

    /**
     * 애플리케이션의 워크플로우 중 주어진 pluginPath 를 쓰는 PLUGIN 노드의 [워크플로우코드, pluginPath].
     * 쓰이는 경로를 지우거나 플러그인을 삭제해서 워크플로우가 조용히 깨지지 않도록 막는 데 쓴다.
     */
    @Query("select distinct w.workflowCode, n.functionName from WorkflowNode n, Workflow w " +
            "where n.workflowId = w.id and w.applicationId = :applicationId and n.functionType = :functionType and n.functionName in :paths")
    List<Object[]> findWorkflowUsages(@Param("applicationId") String applicationId,
                                      @Param("functionType") FunctionType functionType,
                                      @Param("paths") Collection<String> paths);
}
