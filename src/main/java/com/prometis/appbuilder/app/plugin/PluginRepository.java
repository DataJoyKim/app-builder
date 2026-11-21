package com.prometis.appbuilder.app.plugin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PluginRepository extends JpaRepository<Plugin, Long> {
    List<Plugin> findByApplicationIdOrderByPluginIdAsc(String applicationId);

    Optional<Plugin> findByApplicationIdAndPluginId(String applicationId, String pluginId);

    long countByApplicationId(String applicationId);
}
