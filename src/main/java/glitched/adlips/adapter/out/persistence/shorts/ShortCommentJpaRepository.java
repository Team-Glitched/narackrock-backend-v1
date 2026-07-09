package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.domain.shorts.ShortComment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ShortCommentJpaRepository extends JpaRepository<ShortComment, Long> {

    boolean existsByIdAndShortsIdAndDeletedAtIsNull(Long id, Long shortsId);

    Optional<ShortComment> findByIdAndShortsIdAndDeletedAtIsNull(Long id, Long shortsId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ShortComment c WHERE c.id = :id AND c.shorts.id = :shortsId AND c.deletedAt IS NULL")
    Optional<ShortComment> findActiveByIdAndShortsIdForUpdate(
            @Param("id") Long id, @Param("shortsId") Long shortsId);

    @Modifying
    @Query("UPDATE ShortComment c SET c.replyCount = c.replyCount + :delta WHERE c.id = :id")
    void adjustReplyCount(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE ShortComment c SET c.likeCount = c.likeCount + :delta WHERE c.id = :id")
    void adjustLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE ShortComment c SET c.dislikeCount = c.dislikeCount + :delta WHERE c.id = :id")
    void adjustDislikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Query("SELECT c.likeCount FROM ShortComment c WHERE c.id = :id")
    Integer getLikeCount(@Param("id") Long id);

    @Query("SELECT c.dislikeCount FROM ShortComment c WHERE c.id = :id")
    Integer getDislikeCount(@Param("id") Long id);
}
