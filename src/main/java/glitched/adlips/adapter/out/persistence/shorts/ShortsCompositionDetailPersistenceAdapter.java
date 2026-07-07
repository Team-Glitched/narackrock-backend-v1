package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortsCompositionDetailQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ShortsCompositionDetailPersistenceAdapter implements ShortsCompositionDetailQueryPort {

    private final ShortFormJpaRepository shortFormRepository;

    public ShortsCompositionDetailPersistenceAdapter(ShortFormJpaRepository shortFormRepository) {
        this.shortFormRepository = shortFormRepository;
    }

    @Override
    public Optional<String> findActiveShortTitle(Long shortId) {
        return shortFormRepository.findActiveTitle(shortId);
    }
}
