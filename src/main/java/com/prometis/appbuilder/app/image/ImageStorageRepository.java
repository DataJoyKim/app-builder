package com.prometis.appbuilder.app.image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ImageStorageRepository extends JpaRepository<ImageStorage, Long> {
    String INFO = "select new com.prometis.appbuilder.app.image.ImageInfo(i.id, i.applicationId, i.storageType, i.filename, i.originalFilename, i.fileExtension, i.fileSize) from ImageStorage i";

    @Query(INFO + " where i.applicationId = :applicationId and i.storageType = :storageType order by i.id desc")
    List<ImageInfo> findInfos(@Param("applicationId") String applicationId, @Param("storageType") String storageType);

    @Query(INFO + " where i.applicationId = :applicationId and i.storageType = :storageType and i.filename = :filename")
    Optional<ImageInfo> findInfo(@Param("applicationId") String applicationId, @Param("storageType") String storageType, @Param("filename") String filename);

    @Query(INFO + " where i.id = :id")
    Optional<ImageInfo> findInfo(@Param("id") Long id);

    @Query("select i.filename from ImageStorage i where i.applicationId = :applicationId and i.storageType = :storageType")
    List<String> findFilenames(@Param("applicationId") String applicationId, @Param("storageType") String storageType);

    @Query("select i.content from ImageStorage i where i.id = :id")
    Optional<byte[]> findContent(@Param("id") Long id);

    // 바로 실행되므로 같은 filename 으로 새로 넣어도 유니크 제약에 걸리지 않는다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ImageStorage i where i.applicationId = :applicationId and i.storageType = :storageType and i.filename = :filename")
    int deleteByFilename(@Param("applicationId") String applicationId, @Param("storageType") String storageType, @Param("filename") String filename);

    boolean existsByApplicationIdAndStorageType(String applicationId, String storageType);
}
