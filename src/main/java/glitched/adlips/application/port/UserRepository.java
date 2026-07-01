package glitched.adlips.application.port;

import glitched.adlips.domain.user.User;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
}
