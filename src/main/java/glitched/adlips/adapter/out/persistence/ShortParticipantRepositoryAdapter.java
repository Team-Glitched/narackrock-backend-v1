package glitched.adlips.adapter.out.persistence;

import glitched.adlips.application.port.ShortParticipantRepository;
import glitched.adlips.domain.shorts.ShortParticipant;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class ShortParticipantRepositoryAdapter implements ShortParticipantRepository {

    private final ShortParticipantJpaRepository jpaRepository;

    ShortParticipantRepositoryAdapter(ShortParticipantJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<ShortParticipant> findByShortIds(List<Long> shortIds) {
        if (shortIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByShortIdIn(shortIds);
    }
}
