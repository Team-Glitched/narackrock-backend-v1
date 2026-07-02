package glitched.adlips.application.user.profile.port.out;

import glitched.adlips.domain.user.Profile;
import java.util.Optional;

public interface ProfileRepositoryPort {
    Optional<Profile> findByUserId(Long userId);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndUserIdNot(String nickname, Long userId);

    Profile save(Profile profile);
}
