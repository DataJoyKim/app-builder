package com.prometis.appbuilder.platform.join;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppJoinSettingRepository extends JpaRepository<AppJoinSetting, Long> {
    Optional<AppJoinSetting> findByApplicationId(String applicationId);
}
