package glitched.adlips.application.shorts.port.out;

import java.util.Optional;

public interface ShortsCompositionDetailQueryPort {
    Optional<String> findActiveShortTitle(Long shortId);
}
