package glitched.adlips.application.report.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.report.port.out.ReportQueryItem;
import glitched.adlips.application.report.port.out.ReportQueryPort;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.report.ReportStatus;
import glitched.adlips.domain.report.ReportTargetType;
import glitched.adlips.domain.user.UserRole;
import java.util.List;

public class GetReportsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReportQueryPort reportQueryPort;
    private final UserRepositoryPort userRepository;
    private final TransactionRunner transactionRunner;

    public GetReportsUseCase(
            ReportQueryPort reportQueryPort,
            UserRepositoryPort userRepository
    ) {
        this(reportQueryPort, userRepository, TransactionRunner.direct());
    }

    public GetReportsUseCase(
            ReportQueryPort reportQueryPort,
            UserRepositoryPort userRepository,
            TransactionRunner transactionRunner
    ) {
        this.reportQueryPort = reportQueryPort;
        this.userRepository = userRepository;
        this.transactionRunner = transactionRunner;
    }

    public ReportQueryResult execute(
            Long adminUserId,
            String statusValue,
            String targetTypeValue,
            Long cursor,
            int size
    ) {
        return transactionRunner.readOnly(() -> query(
                adminUserId, statusValue, targetTypeValue, cursor, size));
    }

    private ReportQueryResult query(
            Long adminUserId,
            String statusValue,
            String targetTypeValue,
            Long cursor,
            int size
    ) {
        requireAdmin(adminUserId);
        validatePage(cursor, size);
        ReportStatus status = parse(statusValue, ReportStatus.class, "올바르지 않은 신고 상태입니다.");
        ReportTargetType targetType = parse(
                targetTypeValue, ReportTargetType.class, "올바르지 않은 신고 대상 유형입니다.");

        List<ReportQueryItem> queried = reportQueryPort.findReports(
                status, targetType, cursor, size + 1);
        boolean hasNext = queried.size() > size;
        List<ReportQueryItem> content = hasNext
                ? queried.subList(0, size)
                : queried;
        Long nextCursor = hasNext && !content.isEmpty()
                ? content.getLast().reportId()
                : null;
        return new ReportQueryResult(List.copyOf(content), nextCursor, hasNext);
    }

    private void requireAdmin(Long userId) {
        boolean admin = userRepository.findById(userId)
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .isPresent();
        if (!admin) {
            throw new ReportQueryApplicationException(
                    ReportQueryErrorCode.FORBIDDEN,
                    "관리자만 신고 목록을 조회할 수 있습니다."
            );
        }
    }

    private void validatePage(Long cursor, int size) {
        if ((cursor != null && cursor < 1)
                || size < 1
                || size > MAX_PAGE_SIZE) {
            throw new ReportQueryApplicationException(
                    ReportQueryErrorCode.VALIDATION_ERROR,
                    "cursor와 size 값을 확인해 주세요."
            );
        }
    }

    private <T extends Enum<T>> T parse(
            String value,
            Class<T> enumType,
            String errorMessage
    ) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new ReportQueryApplicationException(
                    ReportQueryErrorCode.VALIDATION_ERROR,
                    errorMessage
            );
        }
    }
}
