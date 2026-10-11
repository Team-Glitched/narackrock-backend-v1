package glitched.adlips.adapter.out.persistence.report;

import glitched.adlips.application.report.port.out.ReportSubmissionPort;
import glitched.adlips.application.report.port.out.ReportTarget;
import glitched.adlips.application.report.port.out.ReportTargetQueryPort;
import glitched.adlips.application.report.port.out.ReportUserRestrictionPort;
import glitched.adlips.domain.report.Report;
import glitched.adlips.domain.report.ReportTargetType;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class ReportSubmissionPersistenceAdapter implements
        ReportTargetQueryPort, ReportSubmissionPort, ReportUserRestrictionPort {

    private final ReportJpaRepository reportRepository;
    private final EntityManager entityManager;

    public ReportSubmissionPersistenceAdapter(
            ReportJpaRepository reportRepository,
            EntityManager entityManager
    ) {
        this.reportRepository = reportRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<ReportTarget> findActiveTarget(
            ReportTargetType targetType,
            Long targetId
    ) {
        String query = switch (targetType) {
            case POST -> """
                    select post.user.id
                      from Post post
                     where post.id = :targetId
                       and post.deletedAt is null
                    """;
            case POST_COMMENT -> """
                    select comment.user.id
                      from PostComment comment
                     where comment.id = :targetId
                       and comment.deletedAt is null
                       and comment.post.deletedAt is null
                    """;
            default -> null;
        };
        if (query == null) {
            return Optional.empty();
        }
        return entityManager.createQuery(query, Long.class)
                .setParameter("targetId", targetId)
                .getResultStream()
                .findFirst()
                .map(ReportTarget::new);
    }

    @Override
    public boolean exists(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId
    ) {
        return reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporterId, targetType, targetId);
    }

    @Override
    public Optional<Long> save(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            String reason,
            String description
    ) {
        User reporter = entityManager.getReference(User.class, reporterId);
        try {
            Report saved = reportRepository.saveAndFlush(
                    new Report(reporter, targetType, targetId, reason, description));
            return Optional.of(saved.getId());
        } catch (DataIntegrityViolationException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isBanned(Long userId, LocalDateTime now) {
        return entityManager.createQuery("""
                        select count(ban) > 0
                          from UserBan ban
                         where ban.user.id = :userId
                           and ban.liftedAt is null
                           and (ban.bannedUntil is null or ban.bannedUntil > :now)
                        """, Boolean.class)
                .setParameter("userId", userId)
                .setParameter("now", now)
                .getSingleResult();
    }
}
