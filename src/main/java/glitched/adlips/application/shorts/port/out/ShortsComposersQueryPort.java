package glitched.adlips.application.shorts.port.out;

import java.util.List;

public interface ShortsComposersQueryPort {
    boolean existsActiveShort(Long shortId);

    List<ShortsComposerQueryItem> findByShortId(Long shortId);
}
