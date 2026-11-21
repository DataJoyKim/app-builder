package com.prometis.appbuilder.app.plugin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PluginFileRepository extends JpaRepository<PluginFile, Long> {
    Optional<PluginFile> findByPluginSeq(Long pluginSeq);

    @Query("select f.content from PluginFile f where f.pluginSeq = :pluginSeq")
    Optional<byte[]> findContent(@Param("pluginSeq") Long pluginSeq);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PluginFile f where f.pluginSeq = :pluginSeq")
    int deleteByPluginSeq(@Param("pluginSeq") Long pluginSeq);
}
