package glitched.adlips.application.port;

import glitched.adlips.domain.user.Profile;
import java.util.Optional;

public interface ProfileRepository {
    Profile save(Profile profile);
    Optional<Profile> findByUserId(Long userId);
    boolean existsByNickname(String nickname);
}
