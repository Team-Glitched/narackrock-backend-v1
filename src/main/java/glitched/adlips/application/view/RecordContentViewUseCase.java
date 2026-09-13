package glitched.adlips.application.view;

import glitched.adlips.application.view.port.out.ContentViewCounterPort;
import glitched.adlips.application.view.port.out.ContentViewExistencePort;
import java.time.Duration;
import java.util.Objects;

public class RecordContentViewUseCase {

    private final ContentViewExistencePort existencePort;
    private final ContentViewCounterPort counterPort;
    private final Duration deduplicationWindow;

    public RecordContentViewUseCase(
            ContentViewExistencePort existencePort,
            ContentViewCounterPort counterPort,
            Duration deduplicationWindow
    ) {
        this.existencePort = Objects.requireNonNull(existencePort);
        this.counterPort = Objects.requireNonNull(counterPort);
        this.deduplicationWindow = requirePositive(deduplicationWindow);
    }

    public ContentViewResult execute(ContentViewTarget target, Long contentId, String viewerId) {
        Objects.requireNonNull(target, "target must not be null");
        if (contentId == null || contentId <= 0) {
            throw new ContentViewApplicationException(
                    ContentViewErrorCode.INVALID_CONTENT_ID,
                    "콘텐츠 ID는 양수여야 합니다."
            );
        }
        if (viewerId == null || viewerId.isBlank()) {
            throw new ContentViewApplicationException(
                    ContentViewErrorCode.VIEWER_ID_REQUIRED,
                    "조회자를 식별할 인증 정보 또는 기기 ID가 필요합니다."
            );
        }
        if (!existencePort.exists(target, contentId)) {
            throw new ContentViewApplicationException(
                    ContentViewErrorCode.CONTENT_NOT_FOUND,
                    "존재하지 않거나 조회할 수 없는 콘텐츠입니다."
            );
        }

        boolean counted = counterPort.recordIfFirst(
                target, contentId, viewerId, deduplicationWindow);
        return new ContentViewResult(target, contentId, counted);
    }

    private static Duration requirePositive(Duration duration) {
        Objects.requireNonNull(duration, "deduplicationWindow must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("deduplicationWindow must be positive");
        }
        return duration;
    }
}
