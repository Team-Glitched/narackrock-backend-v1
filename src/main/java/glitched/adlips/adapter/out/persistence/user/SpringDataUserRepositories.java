package glitched.adlips.adapter.out.persistence.user;

import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserAuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

interface SpringDataProfileRepository extends JpaRepository<Profile, Long> {
    boolean existsByNickname(String nickname);
}

interface SpringDataUserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {
    Optional<UserAuthProvider> findByProviderAndProviderUserId(
            AuthProvider provider,
            String providerUserId
    );
}
