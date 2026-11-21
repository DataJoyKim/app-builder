package com.prometis.appbuilder.app.plugin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PluginServerStateRepository extends JpaRepository<PluginServerState, Long> {
    Optional<PluginServerState> findByServerIdAndApplicationIdAndPluginId(String serverId, String applicationId, String pluginId);

    List<PluginServerState> findByApplicationIdOrderByPluginIdAscServerIdAsc(String applicationId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PluginServerState s where s.applicationId = :applicationId and s.pluginId = :pluginId")
    int deleteByPlugin(@Param("applicationId") String applicationId, @Param("pluginId") String pluginId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PluginServerState s where s.serverId = :serverId and s.applicationId = :applicationId and s.pluginId = :pluginId")
    int deleteByServerAndPlugin(@Param("serverId") String serverId, @Param("applicationId") String applicationId, @Param("pluginId") String pluginId);
}
