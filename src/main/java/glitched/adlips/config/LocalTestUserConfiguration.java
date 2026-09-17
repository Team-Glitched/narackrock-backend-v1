package glitched.adlips.config;

import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.user.User;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local-test")
public class LocalTestUserConfiguration {
    @Bean
    ApplicationRunner localTestUser(UserRepositoryPort users, ProfileRepositoryPort profiles) {
        return args -> {
            User user = users.findById(1L)
                    .orElseGet(() -> users.save(User.create("composition-test@localhost")));
            if (!Long.valueOf(1L).equals(user.getId()) || !user.isActive()) {
                throw new IllegalStateException("로컬 테스트 사용자 ID 1을 만들 수 없습니다. 테스트 DB를 새로 시작해 주세요.");
            }
            if (profiles.findByUserId(1L).isEmpty()) {
                profiles.save(glitched.adlips.domain.user.Profile.create(1L, "로컬 작곡가"));
            }
        };
    }
}
