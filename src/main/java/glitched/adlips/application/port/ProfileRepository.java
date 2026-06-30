package glitched.adlips.application.port;

import glitched.adlips.domain.user.Profile;
import java.util.List;
import java.util.Optional;

public interface ProfileRepository {
    Profile save(Profile profile);
    Optional<Profile> findByUserId(Long userId);
    List<Profile> findByUserIds(List<Long> userIds);
    boolean existsByNickname(String nickname);
}
