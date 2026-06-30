package glitched.adlips.application.user.relation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ProfileQueryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FollowUseCasesTest {

    private SocialStore store;
    private FollowCreateUseCase followCreateUseCase;
    private FollowCancelUseCase followCancelUseCase;
    private FollowingGetListUseCase followingGetListUseCase;
    private FollowerGetListUseCase followerGetListUseCase;

    @BeforeEach
    void setUp() {
        store = new SocialStore();
        followCreateUseCase = new FollowCreateUseCase(store, store, store);
        followCancelUseCase = new FollowCancelUseCase(store, store, store);
        followingGetListUseCase = new FollowingGetListUseCase(store, store, store, new EmptyMedia());
        followerGetListUseCase = new FollowerGetListUseCase(store, store, store, new EmptyMedia());
        store.addUser(1L, "me", 0);
        store.addUser(2L, "target", 4);
    }

    @Test
    void followsUserAndUpdatesBothCounters() {
        FollowResult result = followCreateUseCase.execute(1L, 2L);

        assertTrue(result.isFollowing());
        assertEquals(5, result.targetFollowerCount());
        assertEquals(1, result.myFollowingCount());
        assertTrue(store.exists(1L, 2L));
    }

    @Test
    void rejectsFollowingSelf() {
        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> followCreateUseCase.execute(1L, 1L)
        );

        assertEquals(UserErrorCode.CANNOT_FOLLOW_SELF, exception.getErrorCode());
    }

    @Test
    void rejectsDuplicateFollow() {
        followCreateUseCase.execute(1L, 2L);

        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> followCreateUseCase.execute(1L, 2L)
        );

        assertEquals(UserErrorCode.ALREADY_FOLLOWING, exception.getErrorCode());
    }

    @Test
    void unfollowsUserAndDecreasesBothCounters() {
        followCreateUseCase.execute(1L, 2L);

        FollowResult result = followCancelUseCase.execute(1L, 2L);

        assertFalse(result.isFollowing());
        assertEquals(4, result.targetFollowerCount());
        assertEquals(0, result.myFollowingCount());
    }

    @Test
    void returnsFollowingAndFollowerInRequestedDirection() {
        store.addUser(3L, "follower", 0);
        store.save(1L, 2L);
        store.save(3L, 1L);

        List<UserCard> following = followingGetListUseCase.execute(1L);
        List<UserCard> followers = followerGetListUseCase.execute(1L);

        assertEquals(List.of(2L), following.stream().map(UserCard::userId).toList());
        assertEquals(List.of(3L), followers.stream().map(UserCard::userId).toList());
    }

    static final class SocialStore implements
            UserRepositoryPort,
            ProfileRepositoryPort,
            ProfileQueryPort,
            FollowRepositoryPort {
        private final Map<Long, User> users = new HashMap<>();
        private final Map<Long, Profile> profiles = new HashMap<>();
        private final Set<String> follows = new HashSet<>();

        void addUser(Long id, String nickname, int followerCount) {
            users.put(id, User.create(nickname + "@example.com").withId(id));
            profiles.put(id, Profile.restore(
                    id, nickname, null, nickname + " explanation", "기타",
                    false, followerCount, 0, null
            ));
        }

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
        }

        @Override
        public User save(User user) {
            users.put(user.getId(), user);
            return user;
        }

        @Override
        public Optional<Profile> findByUserId(Long userId) {
            return Optional.ofNullable(profiles.get(userId));
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return profiles.values().stream().anyMatch(profile -> profile.getNickname().equals(nickname));
        }

        @Override
        public boolean existsByNicknameAndUserIdNot(String nickname, Long userId) {
            return profiles.values().stream().anyMatch(
                    profile -> !profile.getUserId().equals(userId) && profile.getNickname().equals(nickname)
            );
        }

        @Override
        public Profile save(Profile profile) {
            profiles.put(profile.getUserId(), profile);
            return profile;
        }

        @Override
        public List<Profile> findAllByUserIds(Collection<Long> userIds) {
            return userIds.stream().map(profiles::get).filter(profile -> profile != null).toList();
        }

        @Override
        public PageResult<Profile> search(String keyword, int page, int size) {
            List<Profile> matches = profiles.values().stream()
                    .filter(profile -> profile.getNickname().contains(keyword)
                            || profile.getPrimaryInstrument().contains(keyword)
                            || profile.getExplanation().contains(keyword))
                    .toList();
            return page(matches, page, size);
        }

        @Override
        public PageResult<Profile> findPopularExcluding(Set<Long> excludedIds, int page, int size) {
            List<Profile> matches = profiles.values().stream()
                    .filter(profile -> !excludedIds.contains(profile.getUserId()))
                    .sorted((left, right) -> Integer.compare(right.getFollowerCount(), left.getFollowerCount()))
                    .toList();
            return page(matches, page, size);
        }

        private PageResult<Profile> page(List<Profile> values, int page, int size) {
            int from = Math.min(page * size, values.size());
            int to = Math.min(from + size, values.size());
            return new PageResult<>(values.subList(from, to), values.size());
        }

        @Override
        public boolean exists(Long followerId, Long followingId) {
            return follows.contains(key(followerId, followingId));
        }

        @Override
        public void save(Long followerId, Long followingId) {
            follows.add(key(followerId, followingId));
        }

        @Override
        public boolean delete(Long followerId, Long followingId) {
            return follows.remove(key(followerId, followingId));
        }

        @Override
        public List<Long> findFollowingIds(Long userId) {
            return relatedIds(userId, true);
        }

        @Override
        public List<Long> findFollowerIds(Long userId) {
            return relatedIds(userId, false);
        }

        private List<Long> relatedIds(Long userId, boolean outgoing) {
            List<Long> result = new ArrayList<>();
            for (String value : follows) {
                String[] ids = value.split(":");
                Long followerId = Long.valueOf(ids[0]);
                Long followingId = Long.valueOf(ids[1]);
                if (outgoing && followerId.equals(userId)) {
                    result.add(followingId);
                } else if (!outgoing && followingId.equals(userId)) {
                    result.add(followerId);
                }
            }
            return result;
        }

        private String key(Long followerId, Long followingId) {
            return followerId + ":" + followingId;
        }

    }

    static final class EmptyMedia implements MediaFileRepositoryPort {
        @Override
        public Optional<MediaFile> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public MediaFile save(MediaFile file) {
            return file;
        }
    }
}
