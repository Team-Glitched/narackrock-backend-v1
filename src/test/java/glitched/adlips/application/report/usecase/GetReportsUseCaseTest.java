package glitched.adlips.application.report.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import glitched.adlips.application.report.port.out.ReportQueryItem;
import glitched.adlips.application.report.port.out.ReportQueryPort;
import glitched.adlips.application.report.port.out.ReportUserSummary;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.report.ReportStatus;
import glitched.adlips.domain.report.ReportTargetType;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetReportsUseCaseTest {

    private ReportQueryStore store;
    private GetReportsUseCase useCase;

    @BeforeEach
    void setUp() {
        store = new ReportQueryStore();
        useCase = new GetReportsUseCase(store, store);
        store.user = User.restore(1L, "admin@test.com", UserRole.ADMIN, null);
    }

    @Test
    void 필터와_cursor를_적용하고_다음_page를_계산한다() {
        store.items.add(item(103L));
        store.items.add(item(102L));
        store.items.add(item(101L));

        ReportQueryResult result = useCase.execute(
                1L, "PENDING", "POST", 104L, 2
        );

        assertEquals(ReportStatus.PENDING, store.status);
        assertEquals(ReportTargetType.POST, store.targetType);
        assertEquals(104L, store.cursor);
        assertEquals(3, store.limit);
        assertEquals(List.of(103L, 102L), result.content().stream()
                .map(ReportQueryItem::reportId).toList());
        assertEquals(102L, result.nextCursor());
        assertTrue(result.hasNext());
    }

    @Test
    void 마지막_page는_다음_cursor가_없다() {
        store.items.add(item(103L));

        ReportQueryResult result = useCase.execute(1L, null, null, null, 20);

        assertEquals(1, result.content().size());
        assertEquals(null, result.nextCursor());
        assertFalse(result.hasNext());
    }

    @Test
    void 관리자가_아니면_조회할_수_없다() {
        store.user = User.restore(2L, "user@test.com", UserRole.USER, null);

        ReportQueryApplicationException exception = assertThrows(
                ReportQueryApplicationException.class,
                () -> useCase.execute(2L, null, null, null, 20)
        );

        assertEquals(ReportQueryErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void 잘못된_filter는_거부한다() {
        ReportQueryApplicationException exception = assertThrows(
                ReportQueryApplicationException.class,
                () -> useCase.execute(1L, "UNKNOWN", null, null, 20)
        );

        assertEquals(ReportQueryErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    private ReportQueryItem item(Long reportId) {
        return new ReportQueryItem(
                reportId,
                ReportTargetType.POST,
                10L,
                "/posts/10",
                false,
                "욕설/비방",
                "상세",
                LocalDateTime.of(2026, 9, 21, 12, 0),
                new ReportUserSummary(2L, "reporter"),
                new ReportUserSummary(3L, "reported"),
                "본문"
        );
    }

    private static final class ReportQueryStore implements
            ReportQueryPort, UserRepositoryPort {
        private final List<ReportQueryItem> items = new ArrayList<>();
        private User user;
        private ReportStatus status;
        private ReportTargetType targetType;
        private Long cursor;
        private int limit;

        @Override
        public List<ReportQueryItem> findReports(
                ReportStatus status,
                ReportTargetType targetType,
                Long cursor,
                int limit
        ) {
            this.status = status;
            this.targetType = targetType;
            this.cursor = cursor;
            this.limit = limit;
            return items.stream().limit(limit).toList();
        }

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(user);
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.empty();
        }

        @Override
        public User save(User user) {
            this.user = user;
            return user;
        }
    }
}
