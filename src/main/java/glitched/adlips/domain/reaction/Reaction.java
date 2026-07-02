package glitched.adlips.domain.reaction;

import glitched.adlips.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reactions")
public class Reaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReactionTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false)
    private ReactionType reactionType;

    protected Reaction() {}

    public static Reaction of(
            Long userId,
            ReactionTargetType targetType,
            Long targetId,
            ReactionType reactionType
    ) {
        Reaction reaction = new Reaction();
        reaction.userId = userId;
        reaction.targetType = targetType;
        reaction.targetId = targetId;
        reaction.reactionType = reactionType;
        return reaction;
    }

    public void changeReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public ReactionTargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public ReactionType getReactionType() { return reactionType; }
}
