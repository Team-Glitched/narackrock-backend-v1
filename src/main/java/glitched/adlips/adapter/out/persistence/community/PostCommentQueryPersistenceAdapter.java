package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.PostCommentQueryItem;
import glitched.adlips.application.community.port.out.PostCommentQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PostCommentQueryPersistenceAdapter implements PostCommentQueryPort {
    private final PostJpaRepository posts;
    private final PostCommentJpaRepository comments;
    private final EntityManager entityManager;

    public PostCommentQueryPersistenceAdapter(PostJpaRepository posts,
            PostCommentJpaRepository comments, EntityManager entityManager) {
        this.posts = posts;
        this.comments = comments;
        this.entityManager = entityManager;
    }

    @Override public boolean existsActivePost(Long postId) {
        return posts.findActiveById(postId).isPresent();
    }
    @Override public boolean existsActiveParentComment(Long parentCommentId, Long postId) {
        return comments.existsActiveByIdAndPostId(parentCommentId, postId);
    }
    private String condition(Long parentId) {
        return " WHERE c.post_id = :postId AND c.deleted_at IS NULL AND "
                + (parentId == null ? "c.parent_comment_id IS NULL" : "c.parent_comment_id = :parentId");
    }
    private Query query(String sql, Long postId, Long parentId) {
        Query query = entityManager.createNativeQuery(sql).setParameter("postId", postId);
        if (parentId != null) query.setParameter("parentId", parentId);
        return query;
    }
    @Override public long countComments(Long postId, Long parentId) {
        return ((Number) query("SELECT COUNT(*) FROM post_comments c" + condition(parentId),
                postId, parentId).getSingleResult()).longValue();
    }
    @Override public List<PostCommentQueryItem> findComments(Long postId, Long parentId, int page, int size) {
        String sql = """
                SELECT c.id, c.parent_comment_id, c.user_id, p.nickname, m.file_url, c.content,
                       (SELECT COUNT(*) FROM post_comments r WHERE r.parent_comment_id = c.id
                        AND r.post_id = c.post_id AND r.deleted_at IS NULL), c.created_at
                FROM post_comments c
                LEFT JOIN profiles p ON p.user_id = c.user_id
                LEFT JOIN media_files m ON m.id = p.profile_image_file_id
                """ + condition(parentId) + " ORDER BY c.created_at ASC, c.id ASC";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query(sql, postId, parentId).setFirstResult(page * size)
                .setMaxResults(size).getResultList();
        return rows.stream().map(r -> new PostCommentQueryItem(((Number) r[0]).longValue(),
                r[1] == null ? null : ((Number) r[1]).longValue(), ((Number) r[2]).longValue(),
                (String) r[3], (String) r[4], (String) r[5], ((Number) r[6]).longValue(),
                r[7] instanceof Timestamp t ? t.toLocalDateTime() : (LocalDateTime) r[7])).toList();
    }
}
