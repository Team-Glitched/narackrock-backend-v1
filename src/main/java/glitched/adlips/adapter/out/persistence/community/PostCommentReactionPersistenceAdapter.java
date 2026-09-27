package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.PostCommentReactionPort;
import glitched.adlips.application.community.port.out.ReactionCounts;
import glitched.adlips.domain.reaction.Reaction;
import glitched.adlips.domain.reaction.ReactionTargetType;
import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PostCommentReactionPersistenceAdapter implements PostCommentReactionPort {

    private final PostCommentJpaRepository commentRepository;
    private final CommunityReactionJpaRepository reactionRepository;

    public PostCommentReactionPersistenceAdapter(
            PostCommentJpaRepository commentRepository,
            CommunityReactionJpaRepository reactionRepository
    ) {
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
    }

    @Override
    public boolean lockActiveComment(Long commentId) {
        return commentRepository.findActiveByIdForUpdate(commentId).isPresent();
    }

    @Override
    public Optional<ReactionType> findReaction(Long userId, Long commentId) {
        return reactionRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, ReactionTargetType.POST_COMMENT, commentId)
                .map(Reaction::getReactionType);
    }

    @Override
    public void saveReaction(Long userId, Long commentId, ReactionType reactionType) {
        reactionRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, ReactionTargetType.POST_COMMENT, commentId)
                .ifPresentOrElse(
                        existing -> existing.changeReactionType(reactionType),
                        () -> reactionRepository.save(
                                Reaction.of(userId, ReactionTargetType.POST_COMMENT, commentId, reactionType)));
    }

    @Override
    public void deleteReaction(Long userId, Long commentId) {
        reactionRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, ReactionTargetType.POST_COMMENT, commentId);
    }

    @Override
    public void adjustReactionCounts(Long commentId, int likeDelta, int dislikeDelta) {
        commentRepository.adjustReactionCounts(commentId, likeDelta, dislikeDelta);
    }

    @Override
    public ReactionCounts findReactionCounts(Long commentId) {
        return commentRepository.findReactionCounts(commentId).orElse(new ReactionCounts(0, 0));
    }
}
