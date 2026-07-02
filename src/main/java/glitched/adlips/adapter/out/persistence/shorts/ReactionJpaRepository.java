package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.domain.reaction.Reaction;
import glitched.adlips.domain.reaction.ReactionTargetType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReactionJpaRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByUserIdAndTargetTypeAndTargetId(
            Long userId, ReactionTargetType targetType, Long targetId);

    void deleteByUserIdAndTargetTypeAndTargetId(
            Long userId, ReactionTargetType targetType, Long targetId);
}
