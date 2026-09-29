package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class PostCommentPersistenceAdapterTest {

    @Autowired PostJpaRepository posts;
    @Autowired PostCommentJpaRepository comments;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    PostCommentPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostCommentPersistenceAdapter(posts, comments, entityManager);
        jdbc.update("INSERT INTO users (id,email,role,created_at) VALUES (1,'comments@test.local','USER',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO galleries (id,name,created_at) VALUES (1,'gallery',CURRENT_TIMESTAMP)");
        jdbc.update("""
                INSERT INTO posts
                    (id,gallery_id,user_id,title,content,view_count,like_count,dislike_count,comment_count,created_at)
                VALUES (1,1,1,'post','body',0,0,0,3,CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO post_comments
                    (id,post_id,user_id,content,reply_count,created_at,parent_comment_id,deleted_at)
                VALUES
                    (1,1,1,'root',1,CURRENT_TIMESTAMP,null,null),
                    (2,1,1,'active reply',0,CURRENT_TIMESTAMP,1,null),
                    (3,1,1,'deleted reply',0,CURRENT_TIMESTAMP,1,CURRENT_TIMESTAMP)
                """);
    }

    @Test
    void softDeleteActiveRepliesDeletesOnlyActiveReplies() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 9, 21, 10, 0);

        int deletedCount = adapter.softDeleteActiveReplies(1L, deletedAt);
        entityManager.flush();
        entityManager.clear();

        assertThat(deletedCount).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM post_comments WHERE parent_comment_id = 1 AND deleted_at IS NULL",
                Long.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT deleted_at FROM post_comments WHERE id = 2",
                LocalDateTime.class)).isEqualTo(deletedAt);
    }
}
