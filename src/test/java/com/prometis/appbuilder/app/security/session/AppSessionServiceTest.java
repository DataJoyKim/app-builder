package com.prometis.appbuilder.app.security.session;

import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:app-session;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
class AppSessionServiceTest {
    @Autowired
    AppSessionService appSessionService;
    @Autowired
    AppSessionRepository appSessionRepository;

    @BeforeEach
    void setUp() {
        appSessionRepository.deleteAll();
    }

    private AuthenticatedUser user(long userId) {
        return AuthenticatedUser.builder()
                .userId(userId)
                .userName("tester")
                .grantedPermissions(new ArrayList<>())
                .build();
    }

    @Test
    void 이미_세션이_있으면_새로_만들지_않는다() {
        AppSession first = appSessionService.registerSession("prometis-hr", user(3L));
        AppSession second = appSessionService.registerSession("prometis-hr", user(3L));

        assertEquals(first.getId(), second.getId());
    }
}
