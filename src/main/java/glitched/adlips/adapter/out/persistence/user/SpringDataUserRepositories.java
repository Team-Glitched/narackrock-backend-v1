package glitched.adlips.adapter.out.persistence.user;

import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserAuthProvider;
import glitched.adlips.domain.user.UserRefreshToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

interface SpringDataUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

interface SpringDataProfileRepository extends JpaRepository<Profile, Long> {
    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndUserIdNot(String nickname, Long userId);

    List<Profile> findAllByUserIdIn(Collection<Long> userIds);

    Page<Profile> findByNicknameContainingIgnoreCaseOrPrimaryInstrumentContainingIgnoreCaseOrExplanationContainingIgnoreCase(
            String nickname,
            String primaryInstrument,
            String explanation,
            Pageable pageable
    );

    Page<Profile> findByUserIdNotInOrderByFollowerCountDesc(Set<Long> excludedIds, Pageable pageable);
}

interface SpringDataUserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {
    Optional<UserAuthProvider> findByProviderAndProviderUserId(
            AuthProvider provider,
            String providerUserId
    );
}

interface SpringDataUserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from UserRefreshToken token where token.tokenHash = :tokenHash")
    Optional<UserRefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<UserRefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);
}
