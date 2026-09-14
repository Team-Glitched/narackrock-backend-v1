package glitched.adlips.application.shorts.port.out;

import java.util.Optional;

public interface ShortsCompositionQueryPort {
    Optional<ShortsCompositionQueryItem> findByShortId(Long shortId);
}
