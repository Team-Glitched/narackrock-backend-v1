package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.domain.community.PostComment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PostCommentJpaRepository extends JpaRepository<PostComment, Long> {

    @Query("""
            SELECT COUNT(c) > 0 FROM PostComment c
             WHERE c.id = :id AND c.post.id = :postId
               AND c.parentComment IS NULL AND c.deletedAt IS NULL
            """)
    boolean existsActiveByIdAndPostId(@Param("id") Long id, @Param("postId") Long postId);

    @Query("SELECT c FROM PostComment c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<PostComment> findActiveById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE PostComment c SET c.replyCount = c.replyCount + :delta WHERE c.id = :id")
    void adjustReplyCount(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("""
            UPDATE PostComment c SET c.deletedAt = :deletedAt
             WHERE c.parentComment.id = :parentId AND c.deletedAt IS NULL
            """)
    int softDeleteActiveReplies(
            @Param("parentId") Long parentId,
            @Param("deletedAt") java.time.LocalDateTime deletedAt);
}
