package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class PostCommentQueryPersistenceAdapterTest {
    @Autowired PostJpaRepository posts;
    @Autowired PostCommentJpaRepository comments;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    PostCommentQueryPersistenceAdapter adapter;
    @BeforeEach void setup() {
        adapter = new PostCommentQueryPersistenceAdapter(posts, comments, em);
        jdbc.update("INSERT INTO users (id,email,role,created_at) VALUES (1,'comments@test.local','USER',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO profiles (user_id,nickname,is_private,follower_count,following_count) VALUES (1,'writer',false,0,0)");
        jdbc.update("INSERT INTO galleries (id,name,created_at) VALUES (1,'gallery',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO posts (id,gallery_id,user_id,title,content,view_count,like_count,dislike_count,comment_count,created_at) VALUES (1,1,1,'post','body',0,0,0,0,CURRENT_TIMESTAMP),(2,1,1,'other','body',0,0,0,0,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO post_comments (id,post_id,user_id,content,reply_count,created_at,parent_comment_id,deleted_at) VALUES (1,1,1,'root',0,'2026-09-17 01:00:00',null,null),(2,1,1,'root2',0,'2026-09-17 01:00:00',null,null),(3,1,1,'reply',0,CURRENT_TIMESTAMP,1,null),(4,1,1,'deleted',0,CURRENT_TIMESTAMP,1,CURRENT_TIMESTAMP),(5,2,1,'other',0,CURRENT_TIMESTAMP,null,null)");
    }
    @Test void rootsAreStablePaginatedAndContainWriter() {
        var list = adapter.findComments(1L,null,0,1);
        assertThat(list).hasSize(1);
        assertThat(list.getFirst().commentId()).isEqualTo(1);
        assertThat(list.getFirst().nickname()).isEqualTo("writer");
        assertThat(list.getFirst().replyCount()).isEqualTo(1);
        assertThat(adapter.findComments(1L,null,1,1).getFirst().commentId()).isEqualTo(2);
        assertThat(adapter.countComments(1L,null)).isEqualTo(2);
    }
    @Test void repliesExcludeDeletedAndOtherPosts() {
        assertThat(adapter.findComments(1L,1L,0,20)).extracting(c -> c.content()).containsExactly("reply");
        assertThat(adapter.countComments(1L,1L)).isEqualTo(1);
        assertThat(adapter.existsActiveParentComment(5L,1L)).isFalse();
        assertThat(adapter.existsActiveParentComment(4L,1L)).isFalse();
    }

    @Test void replyCannotBeUsedAsParentComment() {
        assertThat(adapter.existsActiveParentComment(3L, 1L)).isFalse();
    }
}
