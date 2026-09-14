package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortsSharePort;
import org.springframework.stereotype.Repository;

@Repository
public class ShortsSharePersistenceAdapter implements ShortsSharePort {

    private final ShortFormJpaRepository shortFormRepository;

    public ShortsSharePersistenceAdapter(ShortFormJpaRepository shortFormRepository) {
        this.shortFormRepository = shortFormRepository;
    }

    @Override
    public boolean existsActiveShort(Long shortId) {
        return shortFormRepository.existsByIdAndDeletedAtIsNull(shortId);
    }
}
