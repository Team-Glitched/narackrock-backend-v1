package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.domain.reaction.Reaction;
import glitched.adlips.domain.reaction.ReactionTargetType;
import glitched.adlips.domain.reaction.ReactionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CommunityReactionJpaRepository extends JpaRepository<Reaction, Long> {

    List<Reaction> findAllByUserIdAndTargetTypeAndReactionTypeOrderByIdDesc(Long userId, ReactionTargetType targetType, ReactionType reactionType);

    Optional<Reaction> findByUserIdAndTargetTypeAndTargetId(
            Long userId, ReactionTargetType targetType, Long targetId);

    void deleteByUserIdAndTargetTypeAndTargetId(
            Long userId, ReactionTargetType targetType, Long targetId);
}
