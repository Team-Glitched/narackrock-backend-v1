package glitched.adlips.application.opensource;

import java.util.Optional;

public interface OpenSourceMediaPort {
    record Source(Long ownerId, Long mediaFileId) {}
    Optional<Source> findAvailable(Long id);
}
