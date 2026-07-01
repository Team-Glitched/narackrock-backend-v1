package glitched.adlips.adapter.out.persistence;

import glitched.adlips.application.exception.AlreadyRegisteredException;
import glitched.adlips.application.port.UserRepository;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        try {
            return jpaRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyRegisteredException();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }
}
