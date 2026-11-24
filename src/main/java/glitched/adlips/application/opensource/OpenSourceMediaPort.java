package glitched.adlips.application.opensource;

import java.util.Optional;

public interface OpenSourceMediaPort {
    record Source(Long ownerId, Long mediaFileId, String title, String instrument) {
        public Source(Long ownerId, Long mediaFileId) {
            this(ownerId, mediaFileId, null, null);
        }
    }
    Optional<Source> findAvailable(Long id);
    default Optional<Long> findOwnerId(Long id) {
        return findAvailable(id).map(Source::ownerId);
    }
}
