package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.domain.shorts.ShortForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ShortFormJpaRepository extends JpaRepository<ShortForm, Long> {

    @Modifying
    @Query("UPDATE ShortForm s SET s.likeCount = s.likeCount + :delta WHERE s.id = :shortId")
    void adjustLikeCount(@Param("shortId") Long shortId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE ShortForm s SET s.dislikeCount = s.dislikeCount + :delta WHERE s.id = :shortId")
    void adjustDislikeCount(@Param("shortId") Long shortId, @Param("delta") int delta);

    @Query("SELECT s.likeCount FROM ShortForm s WHERE s.id = :shortId")
    Integer getLikeCount(@Param("shortId") Long shortId);

    @Query("SELECT s.dislikeCount FROM ShortForm s WHERE s.id = :shortId")
    Integer getDislikeCount(@Param("shortId") Long shortId);
}
