package glitched.adlips.adapter.out.persistence.opensource;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "open_source_comment_hides")
class OpenSourceCommentHideEntity {
    @EmbeddedId
    private Key id;

    protected OpenSourceCommentHideEntity() {}

    @Embeddable
    record Key(
            @Column(name = "user_id") Long userId,
            @Column(name = "comment_id") Long commentId
    ) implements Serializable {}
}
