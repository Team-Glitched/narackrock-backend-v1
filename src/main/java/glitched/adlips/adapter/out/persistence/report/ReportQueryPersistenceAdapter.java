package glitched.adlips.adapter.out.persistence.report;

import glitched.adlips.application.report.port.out.ReportQueryItem;
import glitched.adlips.application.report.port.out.ReportQueryPort;
import glitched.adlips.application.report.port.out.ReportUserSummary;
import glitched.adlips.domain.report.Report;
import glitched.adlips.domain.report.ReportStatus;
import glitched.adlips.domain.report.ReportTargetType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ReportQueryPersistenceAdapter implements ReportQueryPort {

    private static final int SUMMARY_MAX_LENGTH = 100;

    private final EntityManager entityManager;

    public ReportQueryPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<ReportQueryItem> findReports(
            ReportStatus status,
            ReportTargetType targetType,
            Long cursor,
            int limit
    ) {
        StringBuilder jpql = new StringBuilder("select report from Report report where 1 = 1");
        if (status != null) {
            jpql.append(" and report.status = :status");
        }
        if (targetType != null) {
            jpql.append(" and report.targetType = :targetType");
        }
        if (cursor != null) {
            jpql.append(" and report.id < :cursor");
        }
        jpql.append(" order by report.id desc");

        TypedQuery<Report> query = entityManager.createQuery(jpql.toString(), Report.class);
        if (status != null) {
            query.setParameter("status", status);
        }
        if (targetType != null) {
            query.setParameter("targetType", targetType);
        }
        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }
        query.setMaxResults(limit);

        return query.getResultList().stream().map(this::toItem).toList();
    }

    private ReportQueryItem toItem(Report report) {
        TargetSnapshot target = findTarget(report.getTargetType(), report.getTargetId())
                .orElseGet(() -> TargetSnapshot.missing(
                        fallbackUrl(report.getTargetType(), report.getTargetId())));
        return new ReportQueryItem(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                target.url(),
                target.deleted(),
                report.getReason(),
                report.getDescription(),
                report.getCreatedAt(),
                userSummary(report.reporterId()),
                target.userId() == null
                        ? null
                        : new ReportUserSummary(target.userId(), target.nickname()),
                contentSummary(report.getTargetType(), target)
        );
    }

    private ReportUserSummary userSummary(Long userId) {
        String nickname = entityManager.createQuery("""
                        select profile.nickname
                          from Profile profile
                         where profile.userId = :userId
                        """, String.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .orElse(null);
        return new ReportUserSummary(userId, nickname);
    }

    private Optional<TargetSnapshot> findTarget(
            ReportTargetType targetType,
            Long targetId
    ) {
        return switch (targetType) {
            case POST -> post(targetId);
            case POST_COMMENT -> postComment(targetId);
            case SHORT -> shortForm(targetId);
            case SHORT_COMMENT -> shortComment(targetId);
            case OPEN_SOURCE -> openSource(targetId);
            case OPEN_SOURCE_COMMENT -> openSourceComment(targetId);
        };
    }

    private Optional<TargetSnapshot> post(Long targetId) {
        return first("""
                select post.user.id,
                       (select profile.nickname from Profile profile where profile.userId = post.user.id),
                       post.content,
                       post.deletedAt
                  from Post post
                 where post.id = :targetId
                """, targetId).map(row -> new TargetSnapshot(
                longValue(row[0]), stringValue(row[1]),
                stringValue(row[2]), dateTimeValue(row[3]),
                "/posts/" + targetId, false));
    }

    private Optional<TargetSnapshot> postComment(Long targetId) {
        return first("""
                select comment.user.id,
                       (select profile.nickname from Profile profile where profile.userId = comment.user.id),
                       comment.post.id,
                       comment.content,
                       comment.deletedAt
                  from PostComment comment
                 where comment.id = :targetId
                """, targetId).map(row -> new TargetSnapshot(
                longValue(row[0]), stringValue(row[1]),
                stringValue(row[3]), dateTimeValue(row[4]),
                "/posts/" + longValue(row[2]) + "?commentId=" + targetId,
                false));
    }

    private Optional<TargetSnapshot> shortForm(Long targetId) {
        return first("""
                select shortForm.user.id,
                       (select profile.nickname from Profile profile where profile.userId = shortForm.user.id),
                       shortForm.title,
                       shortForm.deletedAt
                  from ShortForm shortForm
                 where shortForm.id = :targetId
                """, targetId).map(row -> new TargetSnapshot(
                longValue(row[0]), stringValue(row[1]),
                stringValue(row[2]), dateTimeValue(row[3]),
                "/abyss/" + targetId, false));
    }

    private Optional<TargetSnapshot> shortComment(Long targetId) {
        return first("""
                select comment.user.id,
                       (select profile.nickname from Profile profile where profile.userId = comment.user.id),
                       comment.shorts.id,
                       comment.content,
                       comment.deletedAt
                  from ShortComment comment
                 where comment.id = :targetId
                """, targetId).map(row -> new TargetSnapshot(
                longValue(row[0]), stringValue(row[1]),
                stringValue(row[3]), dateTimeValue(row[4]),
                "/abyss/" + longValue(row[2]) + "?commentId=" + targetId,
                false));
    }

    private Optional<TargetSnapshot> openSource(Long targetId) {
        return first("""
                select source.ownerId,
                       (select profile.nickname from Profile profile where profile.userId = source.ownerId),
                       source.title,
                       source.deletedAt
                  from OpenSourceEntity source
                 where source.id = :targetId
                   and source.ownerId is not null
                """, targetId).map(row -> new TargetSnapshot(
                longValue(row[0]), stringValue(row[1]),
                stringValue(row[2]), dateTimeValue(row[3]),
                "/sources/" + targetId, false));
    }

    private Optional<TargetSnapshot> openSourceComment(Long targetId) {
        return first("""
                select comment.userId,
                       (select profile.nickname from Profile profile where profile.userId = comment.userId),
                       comment.openSourceId,
                       comment.content,
                       comment.deletedAt
                  from OpenSourceCommentEntity comment
                 where comment.id = :targetId
                """, targetId).map(row -> new TargetSnapshot(
                longValue(row[0]), stringValue(row[1]),
                stringValue(row[3]), dateTimeValue(row[4]),
                "/sources/" + longValue(row[2]) + "/comments?commentId=" + targetId,
                false));
    }

    private Optional<Object[]> first(String jpql, Long targetId) {
        return entityManager.createQuery(jpql, Object[].class)
                .setParameter("targetId", targetId)
                .getResultStream()
                .findFirst();
    }

    private String contentSummary(
            ReportTargetType targetType,
            TargetSnapshot target
    ) {
        if (target.deleted()) {
            return switch (targetType) {
                case POST -> "삭제된 게시글입니다.";
                case POST_COMMENT, SHORT_COMMENT, OPEN_SOURCE_COMMENT -> "삭제된 댓글입니다.";
                case SHORT -> "삭제된 숏폼입니다.";
                case OPEN_SOURCE -> "삭제된 샘플입니다.";
            };
        }
        if (target.content() == null || target.content().length() <= SUMMARY_MAX_LENGTH) {
            return target.content();
        }
        return target.content().substring(0, SUMMARY_MAX_LENGTH) + "...";
    }

    private String fallbackUrl(ReportTargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> "/posts/" + targetId;
            case POST_COMMENT -> "/posts/comments/" + targetId;
            case SHORT -> "/abyss/" + targetId;
            case SHORT_COMMENT -> "/abyss/comments/" + targetId;
            case OPEN_SOURCE -> "/sources/" + targetId;
            case OPEN_SOURCE_COMMENT -> "/sources/comments/" + targetId;
        };
    }

    private Long longValue(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime dateTimeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.time.Instant instant) {
            return LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
        }
        return (LocalDateTime) value;
    }

    private record TargetSnapshot(
            Long userId,
            String nickname,
            String content,
            LocalDateTime deletedAt,
            String url,
            boolean missing
    ) {
        private boolean deleted() {
            return missing || deletedAt != null;
        }

        private static TargetSnapshot missing(String url) {
            return new TargetSnapshot(null, null, null, null, url, true);
        }
    }
}
