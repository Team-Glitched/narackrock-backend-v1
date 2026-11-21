package glitched.adlips.adapter.out.persistence.report;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.report.port.out.ReportQueryItem;
import glitched.adlips.domain.report.ReportStatus;
import glitched.adlips.domain.report.ReportTargetType;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ReportQueryPersistenceAdapterTest {

    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ReportQueryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ReportQueryPersistenceAdapter(entityManager);
        jdbcTemplate.update("""
                INSERT INTO users (id, email, role, created_at)
                VALUES
                    (1, 'reporter@test.com', 'USER', CURRENT_TIMESTAMP),
                    (2, 'post-owner@test.com', 'USER', CURRENT_TIMESTAMP),
                    (3, 'comment-owner@test.com', 'USER', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO profiles (
                    user_id, nickname, is_private, follower_count,
                    following_count, is_muted
                ) VALUES
                    (1, 'report_user', false, 0, 0, false),
                    (2, 'post_owner', false, 0, 0, false),
                    (3, 'comment_owner', false, 0, 0, false)
                """);
        jdbcTemplate.update("INSERT INTO galleries (id, name, created_at) VALUES (1, 'gallery', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, created_at
                ) VALUES (10, 1, 2, 'post', '신고 대상 게시글 본문', 0, 0, 0, 0, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count,
                    contribution_count, created_at
                ) VALUES (12, 2, 'short title', 100, 'COMPLETED', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO shorts_comments (
                    id, shorts_id, user_id, content, like_count,
                    dislike_count, reply_count, deleted_at, created_at
                ) VALUES (20, 12, 3, '삭제된 댓글', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO media_files (id, owner_id, file_url, file_type, status, created_at)
                VALUES (101, 2, 'http://cdn.test/sample.wav', 'AUDIO', 'READY', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO open_sources (
                    id, owner_id, title, description, media_file_id, is_public, created_at, updated_at
                ) VALUES (30, 2, '신고 대상 샘플', '설명', 101, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO open_source_comments (
                    id, open_source_id, user_id, content, created_at
                ) VALUES (40, 30, 3, '신고 대상 샘플 댓글', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO reports (
                    id, reporter_id, target_type, target_id, reason,
                    description, status, created_at
                ) VALUES
                    (101, 1, 'POST', 10, '광고', '광고성 게시글', 'PENDING', '2026-06-24 14:30:00'),
                    (102, 1, 'SHORT_COMMENT', 20, '욕설', '욕설 댓글', 'PENDING', '2026-06-24 14:35:00'),
                    (103, 1, 'OPEN_SOURCE', 30, '저작권', '샘플 신고', 'PENDING', '2026-06-24 14:40:00'),
                    (104, 1, 'OPEN_SOURCE_COMMENT', 40, '스팸', '샘플 댓글 신고', 'PENDING', '2026-06-24 14:45:00')
                """);
        entityManager.clear();
    }

    @Test
    void 신고_대상별_url_작성자_요약을_조회한다() {
        List<ReportQueryItem> result = adapter.findReports(
                ReportStatus.PENDING, null, null, 20);

        assertThat(result).extracting(ReportQueryItem::reportId)
                .containsExactly(104L, 103L, 102L, 101L);
        assertThat(result.get(0).targetUrl()).isEqualTo("/sources/30/comments?commentId=40");
        assertThat(result.get(0).targetDeleted()).isFalse();
        assertThat(result.get(0).contentSummary()).isEqualTo("신고 대상 샘플 댓글");
        assertThat(result.get(0).reportedUser().nickname()).isEqualTo("comment_owner");
        assertThat(result.get(1).targetUrl()).isEqualTo("/sources/30");
        assertThat(result.get(1).targetDeleted()).isFalse();
        assertThat(result.get(1).contentSummary()).isEqualTo("신고 대상 샘플");
        assertThat(result.get(2).targetUrl()).isEqualTo("/abyss/12?commentId=20");
        assertThat(result.get(2).targetDeleted()).isTrue();
        assertThat(result.get(2).contentSummary()).isEqualTo("삭제된 댓글입니다.");
        assertThat(result.get(2).reportedUser().nickname()).isEqualTo("comment_owner");
        assertThat(result.getLast().targetUrl()).isEqualTo("/posts/10");
        assertThat(result.getLast().contentSummary()).isEqualTo("신고 대상 게시글 본문");
        assertThat(result.getLast().reporter().nickname()).isEqualTo("report_user");
    }

    @Test
    void targetType과_cursor를_적용한다() {
        assertThat(adapter.findReports(
                ReportStatus.PENDING, ReportTargetType.POST, 102L, 20
        )).extracting(ReportQueryItem::reportId).containsExactly(101L);
        assertThat(adapter.findReports(
                ReportStatus.PENDING, ReportTargetType.OPEN_SOURCE_COMMENT, null, 20
        )).extracting(ReportQueryItem::reportId).containsExactly(104L);
    }
}
