package glitched.adlips.adapter.out.persistence;

import glitched.adlips.domain.user.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProfileJpaRepository extends JpaRepository<Profile, Long> {
    boolean existsByNickname(String nickname);
}
