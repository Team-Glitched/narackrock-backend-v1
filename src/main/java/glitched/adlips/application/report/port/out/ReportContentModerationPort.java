package glitched.adlips.application.report.port.out;

import glitched.adlips.domain.report.ReportTargetType;
import java.time.LocalDateTime;
import java.util.Optional;

/** 신고 처리 시 신고 대상 콘텐츠에 조치를 실행한다. */
public interface ReportContentModerationPort {
    /** 삭제 여부와 관계없이 콘텐츠 작성자를 찾는다. */
    Optional<Long> findOwnerId(ReportTargetType targetType, Long targetId);

    /**
     * 작성자 삭제와 같은 규칙으로 소프트 삭제한다. 이미 삭제됐거나 없으면 아무것도 하지 않는다.
     * SHORT는 지원하지 않는다.
     */
    void deleteContent(ReportTargetType targetType, Long targetId, LocalDateTime now);
}
