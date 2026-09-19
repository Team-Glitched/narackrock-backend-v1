package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.domain.shorts.ShortParticipant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ShortParticipantJpaRepository extends JpaRepository<ShortParticipant, Long> {

    long countByShortsId(Long shortId);
    List<ShortParticipant> findByShortsId(Long shortId);
}
