package glitched.adlips.adapter.out.persistence;

import glitched.adlips.domain.shorts.ShortParticipant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ShortParticipantJpaRepository extends JpaRepository<ShortParticipant, Long> {

    @Query("SELECT p FROM ShortParticipant p JOIN FETCH p.user WHERE p.shorts.id IN :shortIds")
    List<ShortParticipant> findByShortIdIn(@Param("shortIds") List<Long> shortIds);
}
