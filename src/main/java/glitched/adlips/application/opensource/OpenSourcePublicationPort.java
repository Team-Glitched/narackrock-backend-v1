package glitched.adlips.application.opensource;

import glitched.adlips.domain.opensource.OpenSourcePublication;
import java.util.Optional;

public interface OpenSourcePublicationPort {
    Optional<OpenSourcePublication> findOwned(Long id, Long ownerId, boolean forUpdate);
    OpenSourcePublication save(OpenSourcePublication publication);
}
