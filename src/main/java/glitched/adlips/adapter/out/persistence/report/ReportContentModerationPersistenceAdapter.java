package glitched.adlips.adapter.out.persistence.report;

import glitched.adlips.application.report.port.out.ReportContentModerationPort;
import glitched.adlips.domain.report.ReportTargetType;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ReportContentModerationPersistenceAdapter implements ReportContentModerationPort {

    private final EntityManager entityManager;

    public ReportContentModerationPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Long> findOwnerId(ReportTargetType targetType, Long targetId) {
        String query = switch (targetType) {
            case POST -> "SELECT t.user.id FROM Post t WHERE t.id = :id";
            case POST_COMMENT -> "SELECT t.user.id FROM PostComment t WHERE t.id = :id";
            case SHORT -> "SELECT t.user.id FROM ShortForm t WHERE t.id = :id";
            case SHORT_COMMENT -> "SELECT t.user.id FROM ShortComment t WHERE t.id = :id";
            case OPEN_SOURCE -> "SELECT t.ownerId FROM OpenSourceEntity t WHERE t.id = :id";
            case OPEN_SOURCE_COMMENT -> "SELECT t.userId FROM OpenSourceCommentEntity t WHERE t.id = :id";
        };
        return entityManager.createQuery(query, Long.class)
                .setParameter("id", targetId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public void deleteContent(ReportTargetType targetType, Long targetId, LocalDateTime now) {
        switch (targetType) {
            case POST -> softDelete("Post", targetId, now);
            case POST_COMMENT -> deletePostComment(targetId, now);
            case SHORT_COMMENT -> deleteShortComment(targetId, now);
            case OPEN_SOURCE -> deleteOpenSource(targetId, now);
            case OPEN_SOURCE_COMMENT -> deleteOpenSourceComment(targetId, now);
            case SHORT -> throw new UnsupportedOperationException("숏폼 삭제는 지원하지 않습니다.");
        }
    }

    private void deleteOpenSource(Long openSourceId, LocalDateTime now) {
        Instant deletedAt = now.toInstant(ZoneOffset.UTC);
        entityManager.createQuery(
                        "UPDATE OpenSourceEntity t SET t.deletedAt = :now, t.updatedAt = :now"
                                + " WHERE t.id = :id AND t.deletedAt IS NULL")
                .setParameter("now", deletedAt)
                .setParameter("id", openSourceId)
                .executeUpdate();
    }

    private void deleteOpenSourceComment(Long commentId, LocalDateTime now) {
        Instant deletedAt = now.toInstant(ZoneOffset.UTC);
        entityManager.createQuery(
                        "UPDATE OpenSourceCommentEntity c SET c.deletedAt = :now"
                                + " WHERE (c.id = :id OR c.parentCommentId = :id) AND c.deletedAt IS NULL")
                .setParameter("now", deletedAt)
                .setParameter("id", commentId)
                .executeUpdate();
    }

    /** DeletePostCommentUseCase와 같은 규칙: 최상위 댓글은 답글까지 지우고, 답글은 부모 답글 수를 줄인다. */
    private void deletePostComment(Long commentId, LocalDateTime now) {
        Optional<Object[]> comment = findActive(
                "SELECT c.post.id, parent.id FROM PostComment c"
                        + " LEFT JOIN c.parentComment parent WHERE c.id = :id AND c.deletedAt IS NULL", commentId);
        if (comment.isEmpty()) {
            return;
        }
        Long postId = (Long) comment.get()[0];
        Long parentId = (Long) comment.get()[1];
        softDelete("PostComment", commentId, now);
        int deletedCount = 1;
        if (parentId == null) {
            deletedCount += entityManager.createQuery("UPDATE PostComment c SET c.deletedAt = :now"
                            + " WHERE c.parentComment.id = :id AND c.deletedAt IS NULL")
                    .setParameter("now", now)
                    .setParameter("id", commentId)
                    .executeUpdate();
        } else {
            adjust("UPDATE PostComment c SET c.replyCount = c.replyCount + :delta WHERE c.id = :id", parentId, -1);
        }
        adjust("UPDATE Post p SET p.commentCount = p.commentCount + :delta WHERE p.id = :id", postId, -deletedCount);
    }

    /** DeleteShortCommentUseCase와 같은 규칙: 최상위 댓글은 답글도 함께 지운다. */
    private void deleteShortComment(Long commentId, LocalDateTime now) {
        Optional<Object[]> comment = findActive(
                "SELECT c.shorts.id, parent.id FROM ShortComment c"
                        + " LEFT JOIN c.parentComment parent WHERE c.id = :id AND c.deletedAt IS NULL", commentId);
        if (comment.isEmpty()) {
            return;
        }
        Long shortId = (Long) comment.get()[0];
        Long parentId = (Long) comment.get()[1];
        softDelete("ShortComment", commentId, now);
        int deletedCount = 1;
        if (parentId == null) {
            deletedCount += entityManager.createQuery("UPDATE ShortComment c SET c.deletedAt = :now"
                            + " WHERE c.parentComment.id = :id AND c.deletedAt IS NULL")
                    .setParameter("now", now)
                    .setParameter("id", commentId)
                    .executeUpdate();
        } else {
            adjust("UPDATE ShortComment c SET c.replyCount = c.replyCount + :delta WHERE c.id = :id", parentId, -1);
        }
        adjust("UPDATE ShortForm s SET s.commentCount = s.commentCount + :delta WHERE s.id = :id", shortId, -deletedCount);
    }

    private Optional<Object[]> findActive(String query, Long id) {
        List<Object[]> rows = entityManager.createQuery(query, Object[].class)
                .setParameter("id", id)
                .getResultList();
        return rows.stream().findFirst();
    }

    private void softDelete(String entity, Long id, LocalDateTime now) {
        entityManager.createQuery("UPDATE " + entity + " t SET t.deletedAt = :now WHERE t.id = :id AND t.deletedAt IS NULL")
                .setParameter("now", now)
                .setParameter("id", id)
                .executeUpdate();
    }

    private void adjust(String query, Long id, int delta) {
        entityManager.createQuery(query)
                .setParameter("id", id)
                .setParameter("delta", delta)
                .executeUpdate();
    }
}
