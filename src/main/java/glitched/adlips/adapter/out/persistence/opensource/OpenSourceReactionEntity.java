package glitched.adlips.adapter.out.persistence.opensource;

import glitched.adlips.application.opensource.OpenSourceInteractionPort.Target;
import glitched.adlips.domain.reaction.ReactionType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="open_source_reactions",uniqueConstraints=@UniqueConstraint(name="uk_os_reaction_user_target",columnNames={"user_id","target_type","target_id"}),
        indexes=@Index(name="idx_os_reaction_target_type",columnList="target_type,target_id,reaction_type"))
class OpenSourceReactionEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="user_id",nullable=false) Long userId;
    @Enumerated(EnumType.STRING) @Column(name="target_type",nullable=false) Target target;
    @Column(name="target_id",nullable=false) Long targetId;
    @Enumerated(EnumType.STRING) @Column(name="reaction_type",nullable=false) ReactionType reaction;
    @Column(name="created_at",nullable=false) Instant createdAt;
    protected OpenSourceReactionEntity() {}
    OpenSourceReactionEntity(Long userId,Target target,Long targetId,ReactionType reaction,Instant now) {
        this.userId=userId;this.target=target;this.targetId=targetId;this.reaction=reaction;this.createdAt=now;
    }
}
