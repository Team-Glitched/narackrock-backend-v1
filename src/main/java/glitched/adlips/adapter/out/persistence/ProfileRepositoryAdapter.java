package glitched.adlips.adapter.out.persistence;

import glitched.adlips.application.exception.DuplicateNicknameException;
import glitched.adlips.application.port.ProfileRepository;
import glitched.adlips.domain.user.Profile;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class ProfileRepositoryAdapter implements ProfileRepository {

    private final ProfileJpaRepository jpaRepository;

    ProfileRepositoryAdapter(ProfileJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Profile save(Profile profile) {
        try {
            return jpaRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateNicknameException();
        }
    }

    @Override
    public Optional<Profile> findByUserId(Long userId) {
        return jpaRepository.findById(userId);
    }

    @Override
    public List<Profile> findByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllByIdIn(userIds);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return jpaRepository.existsByNickname(nickname);
    }
}
