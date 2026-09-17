package glitched.adlips.config;

import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.user.User;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local-test")
public class LocalTestUserConfiguration {
    @Bean
    ApplicationRunner localTestUser(UserRepositoryPort users) {
        return args -> {
            if (users.findById(1L).isPresent()) return;
            User created = users.save(User.create("composition-test@localhost"));
            if (!Long.valueOf(1L).equals(created.getId())) {
                throw new IllegalStateException("로컬 테스트 사용자 ID 1을 만들 수 없습니다. 테스트 DB를 새로 시작해 주세요.");
            }
        };
    }
}
