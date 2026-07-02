package glitched.adlips.application.user.common.port.out;

import glitched.adlips.domain.user.User;
import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    User save(User user);
}
