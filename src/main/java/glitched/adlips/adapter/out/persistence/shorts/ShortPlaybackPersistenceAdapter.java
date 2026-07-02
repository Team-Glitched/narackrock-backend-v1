package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortPlaybackPort;
import org.springframework.stereotype.Repository;

@Repository
public class ShortPlaybackPersistenceAdapter implements ShortPlaybackPort {

    private final ShortFormJpaRepository shortFormRepository;

    public ShortPlaybackPersistenceAdapter(ShortFormJpaRepository shortFormRepository) {
        this.shortFormRepository = shortFormRepository;
    }

    @Override
    public boolean existsActiveShort(Long shortId) {
        return shortFormRepository.existsByIdAndDeletedAtIsNull(shortId);
    }
}
