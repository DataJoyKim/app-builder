package com.prometis.appbuilder.app.security.repository;

import com.prometis.appbuilder.app.security.domain.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {
}
