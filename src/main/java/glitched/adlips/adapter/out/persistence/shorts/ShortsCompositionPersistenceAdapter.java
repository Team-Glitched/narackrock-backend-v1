package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ShortsCompositionPersistenceAdapter implements ShortsCompositionQueryPort {

    private final ShortFormJpaRepository shortFormRepository;

    public ShortsCompositionPersistenceAdapter(ShortFormJpaRepository shortFormRepository) {
        this.shortFormRepository = shortFormRepository;
    }

    @Override
    public Optional<ShortsCompositionQueryItem> findByShortId(Long shortId) {
        return shortFormRepository.findCompositionInfo(shortId);
    }
}
