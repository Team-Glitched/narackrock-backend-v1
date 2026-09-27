package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.PostReactionPort;
import glitched.adlips.application.community.port.out.ReactionCounts;
import glitched.adlips.domain.reaction.Reaction;
import glitched.adlips.domain.reaction.ReactionTargetType;
import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PostReactionPersistenceAdapter implements PostReactionPort {

    private final PostJpaRepository postRepository;
    private final CommunityReactionJpaRepository reactionRepository;

    public PostReactionPersistenceAdapter(
            PostJpaRepository postRepository,
            CommunityReactionJpaRepository reactionRepository
    ) {
        this.postRepository = postRepository;
        this.reactionRepository = reactionRepository;
    }

    @Override
    public boolean lockActivePost(Long postId) {
        return postRepository.findActiveByIdForUpdate(postId).isPresent();
    }

    @Override
    public Optional<ReactionType> findReaction(Long userId, Long postId) {
        return reactionRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, ReactionTargetType.POST, postId)
                .map(Reaction::getReactionType);
    }

    @Override
    public void saveReaction(Long userId, Long postId, ReactionType reactionType) {
        reactionRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, ReactionTargetType.POST, postId)
                .ifPresentOrElse(
                        existing -> existing.changeReactionType(reactionType),
                        () -> reactionRepository.save(
                                Reaction.of(userId, ReactionTargetType.POST, postId, reactionType)));
    }

    @Override
    public void deleteReaction(Long userId, Long postId) {
        reactionRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, ReactionTargetType.POST, postId);
    }

    @Override
    public void adjustReactionCounts(Long postId, int likeDelta, int dislikeDelta) {
        postRepository.adjustReactionCounts(postId, likeDelta, dislikeDelta);
    }

    @Override
    public ReactionCounts findReactionCounts(Long postId) {
        return postRepository.findReactionCounts(postId).orElse(new ReactionCounts(0, 0));
    }
}
