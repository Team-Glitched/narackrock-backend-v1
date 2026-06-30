package glitched.adlips.adapter.out.persistence;

import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.shorts.ShortStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ShortFormJpaRepository extends JpaRepository<ShortForm, Long> {

    @Query("SELECT s FROM ShortForm s JOIN FETCH s.user WHERE s.status = :status ORDER BY s.id DESC LIMIT :limit")
    List<ShortForm> findFirstPage(@Param("status") ShortStatus status, @Param("limit") int limit);

    @Query("SELECT s FROM ShortForm s JOIN FETCH s.user WHERE s.status = :status AND s.id < :cursor ORDER BY s.id DESC LIMIT :limit")
    List<ShortForm> findNextPage(@Param("cursor") Long cursor, @Param("status") ShortStatus status, @Param("limit") int limit);
}
