package glitched.adlips.application.report.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import glitched.adlips.application.report.port.out.ReportSubmissionPort;
import glitched.adlips.application.report.port.out.ReportTarget;
import glitched.adlips.application.report.port.out.ReportTargetQueryPort;
import glitched.adlips.application.report.port.out.ReportUserRestrictionPort;
import glitched.adlips.domain.report.ReportTargetType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmitReportUseCaseTest {

    private ReportStore store;
    private SubmitReportUseCase useCase;

    @BeforeEach
    void setUp() {
        store = new ReportStore();
        useCase = new SubmitReportUseCase(
                store,
                store,
                store,
                Clock.fixed(Instant.parse("2026-09-21T03:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void 게시글_신고를_접수한다() {
        store.targets.put(key(ReportTargetType.POST, 10L), new ReportTarget(20L));

        ReportSubmissionResult result = useCase.execute(
                ReportTargetType.POST, 10L, 1L, "욕설/비방", "비방 내용입니다."
        );

        assertEquals(1L, result.reportId());
        assertEquals(ReportTargetType.POST, result.targetType());
        assertEquals(10L, result.targetId());
        assertEquals("욕설/비방", store.savedReason);
        assertEquals("비방 내용입니다.", store.savedDescription);
    }

    @Test
    void 게시글_댓글_신고를_접수한다() {
        store.targets.put(key(ReportTargetType.POST_COMMENT, 11L), new ReportTarget(20L));

        ReportSubmissionResult result = useCase.execute(
                ReportTargetType.POST_COMMENT, 11L, 1L, "욕설/비방", null
        );

        assertEquals(ReportTargetType.POST_COMMENT, result.targetType());
        assertEquals(11L, result.targetId());
    }

    @Test
    void 삭제되었거나_없는_게시글은_신고할_수_없다() {
        ReportSubmissionApplicationException exception = assertThrows(
                ReportSubmissionApplicationException.class,
                () -> useCase.execute(ReportTargetType.POST, 99L, 1L, "스팸", null)
        );

        assertEquals(ReportSubmissionErrorCode.POST_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 본인이_작성한_댓글은_신고할_수_없다() {
        store.targets.put(key(ReportTargetType.POST_COMMENT, 11L), new ReportTarget(1L));

        ReportSubmissionApplicationException exception = assertThrows(
                ReportSubmissionApplicationException.class,
                () -> useCase.execute(ReportTargetType.POST_COMMENT, 11L, 1L, "욕설", null)
        );

        assertEquals(ReportSubmissionErrorCode.CANNOT_REPORT_OWN_COMMENT, exception.getErrorCode());
    }

    @Test
    void 같은_대상을_중복_신고할_수_없다() {
        store.targets.put(key(ReportTargetType.POST, 10L), new ReportTarget(20L));
        store.existingReports.add(key(ReportTargetType.POST, 10L));

        ReportSubmissionApplicationException exception = assertThrows(
                ReportSubmissionApplicationException.class,
                () -> useCase.execute(ReportTargetType.POST, 10L, 1L, "스팸", null)
        );

        assertEquals(ReportSubmissionErrorCode.ALREADY_REPORTED, exception.getErrorCode());
    }

    @Test
    void 이용_정지된_사용자는_신고할_수_없다() {
        store.banned = true;

        ReportSubmissionApplicationException exception = assertThrows(
                ReportSubmissionApplicationException.class,
                () -> useCase.execute(ReportTargetType.POST, 10L, 1L, "스팸", null)
        );

        assertEquals(ReportSubmissionErrorCode.BANNED_USER_ACCESS, exception.getErrorCode());
        assertEquals(LocalDateTime.of(2026, 9, 21, 3, 0), store.restrictionCheckedAt);
    }

    @Test
    void 공백인_신고_사유는_거부한다() {
        ReportSubmissionApplicationException exception = assertThrows(
                ReportSubmissionApplicationException.class,
                () -> useCase.execute(ReportTargetType.POST, 10L, 1L, "  ", null)
        );

        assertEquals(ReportSubmissionErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    private String key(ReportTargetType targetType, Long targetId) {
        return targetType + ":" + targetId;
    }

    private final class ReportStore implements
            ReportTargetQueryPort, ReportSubmissionPort, ReportUserRestrictionPort {
        private final Map<String, ReportTarget> targets = new HashMap<>();
        private final Set<String> existingReports = new HashSet<>();
        private boolean banned;
        private LocalDateTime restrictionCheckedAt;
        private String savedReason;
        private String savedDescription;

        @Override
        public Optional<ReportTarget> findActiveTarget(ReportTargetType targetType, Long targetId) {
            return Optional.ofNullable(targets.get(key(targetType, targetId)));
        }

        @Override
        public boolean exists(Long reporterId, ReportTargetType targetType, Long targetId) {
            return existingReports.contains(key(targetType, targetId));
        }

        @Override
        public Optional<Long> save(
                Long reporterId,
                ReportTargetType targetType,
                Long targetId,
                String reason,
                String description
        ) {
            savedReason = reason;
            savedDescription = description;
            existingReports.add(key(targetType, targetId));
            return Optional.of(1L);
        }

        @Override
        public boolean isBanned(Long userId, LocalDateTime now) {
            restrictionCheckedAt = now;
            return banned;
        }
    }
}
