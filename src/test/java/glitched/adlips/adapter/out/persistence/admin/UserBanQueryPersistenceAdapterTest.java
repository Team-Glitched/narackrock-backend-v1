package glitched.adlips.adapter.out.persistence.admin;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.admin.UserBan;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class UserBanQueryPersistenceAdapterTest {

    @Autowired UserBanJpaRepository userBanRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    UserBanQueryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserBanQueryPersistenceAdapter(userBanRepository);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'commenter@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (2, 'admin@test.com', 'ADMIN', CURRENT_TIMESTAMP)");
    }

    @Test
    void 해제되지_않은_무기한_이용정지는_활성_상태다() {
        jdbcTemplate.update("""
                INSERT INTO user_bans (user_id, admin_id, reason, banned_until, lifted_at, created_at)
                VALUES (1, 2, 'policy violation', NULL, NULL, CURRENT_TIMESTAMP)
                """);

        assertThat(adapter.isBanned(1L, LocalDateTime.of(2026, 7, 8, 19, 0))).isTrue();
    }

    @Test
    void ERD에_정의된_이용정지_인덱스를_매핑한다() {
        Table table = UserBan.class.getAnnotation(Table.class);

        assertThat(Arrays.stream(table.indexes()).map(index -> index.columnList()))
                .containsExactlyInAnyOrder("user_id, lifted_at", "admin_id");
    }

    @Test
    void 만료되거나_해제된_이용정지는_활성_상태가_아니다() {
        jdbcTemplate.update("""
                INSERT INTO user_bans (user_id, admin_id, reason, banned_until, lifted_at, created_at)
                VALUES (1, 2, 'expired', '2026-07-07 19:00:00', NULL, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO user_bans (user_id, admin_id, reason, banned_until, lifted_at, created_at)
                VALUES (1, 2, 'lifted', NULL, '2026-07-08 18:00:00', CURRENT_TIMESTAMP)
                """);

        assertThat(adapter.isBanned(1L, LocalDateTime.of(2026, 7, 8, 19, 0))).isFalse();
    }
}
