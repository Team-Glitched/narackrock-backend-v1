package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.ReactionCounts;
import glitched.adlips.domain.community.PostComment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM PostComment c
             WHERE c.id = :id AND c.deletedAt IS NULL AND c.post.deletedAt IS NULL
            """)
    Optional<PostComment> findActiveByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE PostComment c
               SET c.likeCount = c.likeCount + :likeDelta,
                   c.dislikeCount = c.dislikeCount + :dislikeDelta
             WHERE c.id = :id
            """)
    void adjustReactionCounts(
            @Param("id") Long id, @Param("likeDelta") int likeDelta, @Param("dislikeDelta") int dislikeDelta);

    @Query("""
            SELECT new glitched.adlips.application.community.port.out.ReactionCounts(c.likeCount, c.dislikeCount)
              FROM PostComment c
             WHERE c.id = :id
            """)
    Optional<ReactionCounts> findReactionCounts(@Param("id") Long id);
}
