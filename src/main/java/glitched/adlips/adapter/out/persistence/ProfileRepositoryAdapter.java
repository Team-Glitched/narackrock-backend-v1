package glitched.adlips.adapter.out.persistence;

import glitched.adlips.application.port.ProfileRepository;
import glitched.adlips.domain.user.Profile;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class ProfileRepositoryAdapter implements ProfileRepository {

    private final ProfileJpaRepository jpaRepository;

    ProfileRepositoryAdapter(ProfileJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Profile save(Profile profile) {
        return jpaRepository.save(profile);
    }

    @Override
    public Optional<Profile> findByUserId(Long userId) {
        return jpaRepository.findById(userId);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return jpaRepository.existsByNickname(nickname);
    }
}
