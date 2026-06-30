package glitched.adlips.adapter.out.persistence;

import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
