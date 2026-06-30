package glitched.adlips.adapter.out.persistence;

import glitched.adlips.domain.shorts.ShortComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ShortCommentJpaRepository extends JpaRepository<ShortComment, Long> {

    interface CommentCount {
        Long getShortId();
        Long getCount();
    }

    @Query("SELECT c.shorts.id AS shortId, COUNT(c) AS count FROM ShortComment c WHERE c.shorts.id IN :shortIds GROUP BY c.shorts.id")
    List<CommentCount> countByShortIdIn(@Param("shortIds") List<Long> shortIds);
}
