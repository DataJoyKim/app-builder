package com.prometis.appbuilder.platform.user;

import com.prometis.core.crypto.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sysadmin-initializer;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false",
        "platform.sysadmin.login-id=sysadmin",
        "platform.sysadmin.initial-password=init-pass!"
})
class SysAdminInitializerTest {
    @Autowired
    SysAdminInitializer sysAdminInitializer;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void 기동시_sysadmin이_없으면_설정한_초기비밀번호로_만든다() {
        // 컨텍스트가 뜨면서 이미 한 번 실행됐다.
        User sysadmin = userRepository.findByLoginId("sysadmin").orElseThrow();

        assertTrue(passwordEncoder.matches("init-pass!", sysadmin.getPassword()));
        assertEquals(UserAuthority.PLATFORM_ADMIN.name(), sysadmin.getAuthority());
    }

    @Test
    void sysadmin이_이미_있으면_새로_만들거나_바꾸지_않는다() {
        User before = userRepository.findByLoginId("sysadmin").orElseThrow();
        long countBefore = userRepository.count();

        sysAdminInitializer.run(new DefaultApplicationArguments());

        User after = userRepository.findByLoginId("sysadmin").orElseThrow();
        assertEquals(countBefore, userRepository.count());
        assertEquals(before.getId(), after.getId());
        assertEquals(before.getPassword(), after.getPassword());
    }
}
