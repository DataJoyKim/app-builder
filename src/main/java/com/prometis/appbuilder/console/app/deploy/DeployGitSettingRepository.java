package com.prometis.appbuilder.console.app.deploy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeployGitSettingRepository extends JpaRepository<DeployGitSetting, Long> {
    Optional<DeployGitSetting> findByApplicationId(String applicationId);
}
