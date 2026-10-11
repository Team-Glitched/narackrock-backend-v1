package glitched.adlips.config;

import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.user.User;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("local-test")
public class LocalTestUserConfiguration {
    @Bean
    ApplicationRunner localTestData(
            UserRepositoryPort users,
            ProfileRepositoryPort profiles,
            JdbcTemplate jdbcTemplate
    ) {
        return args -> {
            User user = users.findById(1L)
                    .orElseGet(() -> users.save(User.create("composition-test@localhost")));
            if (!Long.valueOf(1L).equals(user.getId()) || !user.isActive()) {
                throw new IllegalStateException("로컬 테스트 사용자 ID 1을 만들 수 없습니다. 테스트 DB를 새로 시작해 주세요.");
            }
            if (profiles.findByUserId(1L).isEmpty()) {
                profiles.save(glitched.adlips.domain.user.Profile.create(1L, "로컬 작곡가"));
            }
            User otherUser = users.findByEmail("bookmark-observer@localhost")
                    .orElseGet(() -> users.save(User.create("bookmark-observer@localhost")));
            if (profiles.findByUserId(otherUser.getId()).isEmpty()) {
                profiles.save(glitched.adlips.domain.user.Profile.create(otherUser.getId(), "북마크 관찰자"));
            }
            jdbcTemplate.update("""
                    INSERT INTO galleries (id, name, description, created_at)
                    SELECT 1, '기본 커뮤니티', '로컬 TEST 환경의 기본 게시글 커뮤니티입니다.', CURRENT_TIMESTAMP
                    WHERE NOT EXISTS (SELECT 1 FROM galleries WHERE id = 1)
                    """);
            jdbcTemplate.update("""
                    INSERT INTO media_files (id, owner_id, file_url, storage_key, original_filename,
                                             file_type, mime_type, file_size, status, created_at)
                    SELECT 901, 1, 'http://localhost:8081/files/test-short.wav', 'local-test-short',
                           'test-short.wav', 'AUDIO', 'audio/wav', 1, 'READY', CURRENT_TIMESTAMP
                    WHERE NOT EXISTS (SELECT 1 FROM media_files WHERE id = 901)
                    """);
            jdbcTemplate.update("""
                    INSERT INTO shorts (id, user_id, title, media_file_id, status,
                                        view_count, like_count, dislike_count, comment_count,
                                        contribution_count, created_at)
                    SELECT 901, 1, '로컬 테스트 숏폼', 901, 'COMPLETED', 0, 0, 0, 0, 0,
                           CURRENT_TIMESTAMP
                    WHERE NOT EXISTS (SELECT 1 FROM shorts WHERE id = 901)
                    """);
        };
    }
}
