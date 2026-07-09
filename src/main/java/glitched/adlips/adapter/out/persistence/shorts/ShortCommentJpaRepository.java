package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.domain.shorts.ShortComment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ShortCommentJpaRepository extends JpaRepository<ShortComment, Long> {

    boolean existsByIdAndShortsIdAndDeletedAtIsNull(Long id, Long shortsId);

    Optional<ShortComment> findByIdAndShortsIdAndDeletedAtIsNull(Long id, Long shortsId);

    @Modifying
    @Query("UPDATE ShortComment c SET c.replyCount = c.replyCount + :delta WHERE c.id = :id")
    void adjustReplyCount(@Param("id") Long id, @Param("delta") int delta);
}
