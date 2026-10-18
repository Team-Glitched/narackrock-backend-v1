package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.LikedPostQueryPort;
import glitched.adlips.domain.reaction.ReactionTargetType;
import glitched.adlips.domain.reaction.ReactionType;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class LikedPostPersistenceAdapter implements LikedPostQueryPort {
    private final CommunityReactionJpaRepository reactions;
    public LikedPostPersistenceAdapter(CommunityReactionJpaRepository reactions) { this.reactions = reactions; }
    public List<Long> findPostIds(Long userId) {
        return reactions.findAllByUserIdAndTargetTypeAndReactionTypeOrderByIdDesc(userId, ReactionTargetType.POST, ReactionType.LIKE)
                .stream().map(reaction -> reaction.getTargetId()).toList();
    }
}
