package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryPort;

public class ShortsCompositionEntryUseCase {

    private final ShortsCompositionQueryPort queryPort;

    public ShortsCompositionEntryUseCase(ShortsCompositionQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public ShortsCompositionQueryItem execute(Long shortId) {
        ShortsCompositionQueryItem item = queryPort.findByShortId(shortId)
                .orElseThrow(() -> new ShortsCompositionApplicationException(
                        ShortsCompositionErrorCode.SHORT_NOT_FOUND, "숏폼을 찾을 수 없습니다."));

        if (item.projectId() == null || item.projectDeletedAt() != null) {
            throw new ShortsCompositionApplicationException(
                    ShortsCompositionErrorCode.PROJECT_NOT_LINKED, "연결된 작곡 프로젝트를 찾을 수 없습니다.");
        }

        return item;
    }
}
