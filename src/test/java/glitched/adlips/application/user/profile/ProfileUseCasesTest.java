package glitched.adlips.application.user.profile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.dto.request.UserProfileGetRequest;
import glitched.adlips.application.user.profile.dto.request.UserProfileImageUpdateRequest;
import glitched.adlips.application.user.profile.dto.request.UserProfileShareRequest;
import glitched.adlips.application.user.profile.dto.request.UserProfileUpdateRequest;
import glitched.adlips.application.user.profile.dto.response.UserProfileGetResponse;
import glitched.adlips.application.user.profile.dto.response.UserProfileImageUpdateResponse;
import glitched.adlips.application.user.profile.dto.response.UserProfileShareResponse;
import glitched.adlips.application.user.profile.dto.response.UserProfileUpdateResponse;
import glitched.adlips.application.user.profile.port.out.FileStoragePort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileActivityQueryPort;
import glitched.adlips.application.user.profile.port.out.ProfileLinkPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.profile.usecase.ProfileGetUseCase;
import glitched.adlips.application.user.profile.usecase.ProfileImageUpdateUseCase;
import glitched.adlips.application.user.profile.usecase.ProfileShareUseCase;
import glitched.adlips.application.user.profile.usecase.ProfileUpdateUseCase;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfileUseCasesTest {

    private InMemoryUsers users;
    private InMemoryProfiles profiles;
    private InMemoryMedia media;
    private RecordingStorage storage;
    private ProfileGetUseCase profileGetUseCase;
    private ProfileUpdateUseCase profileUpdateUseCase;
    private ProfileImageUpdateUseCase profileImageUpdateUseCase;
    private ProfileShareUseCase profileShareUseCase;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        profiles = new InMemoryProfiles();
        media = new InMemoryMedia();
        storage = new RecordingStorage();
        FollowRepositoryPort follows = new FollowRepositoryPort() {
            @Override
            public boolean exists(Long followerId, Long followingId) {
                return false;
            }

            @Override
            public void save(Long followerId, Long followingId) {
            }

            @Override
            public boolean delete(Long followerId, Long followingId) {
                return false;
            }

            @Override
            public List<Long> findFollowingIds(Long userId) {
                return List.of();
            }

            @Override
            public List<Long> findFollowerIds(Long userId) {
                return List.of();
            }
        };
        ProfileLinkPort links = userId -> "https://app.example.com/users/profiles/" + userId;
        Clock clock = Clock.fixed(Instant.parse("2026-06-30T00:00:00Z"), ZoneOffset.UTC);
        ProfileActivityQueryPort activities = userId -> new UserProfileGetResponse.Activities(
                1,
                List.of(new UserProfileGetResponse.PostActivity(10L, "첫 게시글", null)),
                List.of(new UserProfileGetResponse.ShortActivity(20L, "참여 숏폼", "album.png")),
                List.of(new UserProfileGetResponse.PinnedShortActivity(30L, "고정 숏폼", "album.png", "media.mp4", 0))
        );
        profileGetUseCase = new ProfileGetUseCase(users, profiles, media, follows, activities);
        profileUpdateUseCase = new ProfileUpdateUseCase(users, profiles, clock);
        profileImageUpdateUseCase = new ProfileImageUpdateUseCase(users, profiles, media, storage);
        profileShareUseCase = new ProfileShareUseCase(users, profiles, links);

        users.save(User.create("user@example.com").withId(1L));
        profiles.save(Profile.create(1L, "guitar_moon"));
    }

    @Test
    void returnsProfileWithActivitiesFromQueryPort() {
        UserProfileGetResponse result = profileGetUseCase.execute(new UserProfileGetRequest(1L, 1L));

        assertEquals("guitar_moon", result.nickname());
        assertEquals(0, result.relations().followerCount());
        assertEquals(1, result.activities().postCount());
        assertEquals(List.of(10L), result.activities().posts().stream()
                .map(UserProfileGetResponse.PostActivity::postId)
                .toList());
        assertEquals(List.of(20L), result.activities().participatedShorts().stream()
                .map(UserProfileGetResponse.ShortActivity::shortId)
                .toList());
        assertEquals(List.of(30L), result.activities().pinnedShorts().stream()
                .map(UserProfileGetResponse.PinnedShortActivity::shortId)
                .toList());
    }

    @Test
    void rejectsPrivateProfileForAnotherUser() {
        profiles.save(Profile.restore(1L, "guitar_moon", null, null, null, true, 0, 0, null));

        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> profileGetUseCase.execute(new UserProfileGetRequest(1L, 2L))
        );

        assertEquals(UserErrorCode.PRIVATE_PROFILE, exception.getErrorCode());
    }

    @Test
    void updatesOnlyProvidedProfileFields() {
        profiles.save(Profile.restore(1L, "guitar_moon", null, "old", "기타", false, 0, 0, null));

        UserProfileUpdateResponse result = profileUpdateUseCase.execute(
                new UserProfileUpdateRequest(1L, "new_nickname", null, "new explanation")
        );

        assertEquals("new_nickname", result.nickname());
        assertEquals("기타", result.primaryInstrument());
        assertEquals("new explanation", result.explanation());
    }

    @Test
    void rejectsNicknameOwnedByAnotherUser() {
        users.save(User.create("other@example.com").withId(2L));
        profiles.save(Profile.create(2L, "taken"));

        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> profileUpdateUseCase.execute(
                        new UserProfileUpdateRequest(1L, "taken", null, null)
                )
        );

        assertEquals(UserErrorCode.DUPLICATE_NICKNAME, exception.getErrorCode());
    }

    @Test
    void storesProfileImageAndReturnsLocalUrl() {
        byte[] image = new byte[]{1, 2, 3};

        UserProfileImageUpdateResponse result = profileImageUpdateUseCase.execute(
                new UserProfileImageUpdateRequest(1L, "avatar.png", "image/png", image)
        );

        assertEquals("http://localhost:8080/files/profile-image.png", result.profileImageUrl());
        assertArrayEquals(image, storage.lastContent);
        assertEquals(1L, profiles.findByUserId(1L).orElseThrow().getProfileImageFileId());
    }

    @Test
    void rejectsUnsupportedProfileImage() {
        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> profileImageUpdateUseCase.execute(
                        new UserProfileImageUpdateRequest(
                                1L, "avatar.txt", "text/plain", new byte[]{1}
                        )
                )
        );

        assertEquals(UserErrorCode.INVALID_IMAGE_FILE, exception.getErrorCode());
    }

    @Test
    void createsProfileShareLinkForExistingUser() {
        UserProfileShareResponse result = profileShareUseCase.execute(new UserProfileShareRequest(1L));

        assertEquals(1L, result.userId());
        assertEquals("https://app.example.com/users/profiles/1", result.shareUrl());
    }

    private static final class InMemoryUsers implements UserRepositoryPort {
        private final Map<Long, User> values = new HashMap<>();

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(values.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return values.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
        }

        @Override
        public User save(User user) {
            values.put(user.getId(), user);
            return user;
        }
    }

    private static final class InMemoryProfiles implements ProfileRepositoryPort {
        private final Map<Long, Profile> values = new HashMap<>();

        @Override
        public Optional<Profile> findByUserId(Long userId) {
            return Optional.ofNullable(values.get(userId));
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return values.values().stream().anyMatch(profile -> profile.getNickname().equals(nickname));
        }

        @Override
        public boolean existsByNicknameAndUserIdNot(String nickname, Long userId) {
            return values.values().stream().anyMatch(
                    profile -> !profile.getUserId().equals(userId) && profile.getNickname().equals(nickname)
            );
        }

        @Override
        public Profile save(Profile profile) {
            values.put(profile.getUserId(), profile);
            return profile;
        }
    }

    private static final class InMemoryMedia implements MediaFileRepositoryPort {
        private final Map<Long, MediaFile> values = new HashMap<>();
        private long sequence = 1;

        @Override
        public Optional<MediaFile> findById(Long id) {
            return Optional.ofNullable(values.get(id));
        }

        @Override
        public MediaFile save(MediaFile file) {
            MediaFile saved = file.getId() == null ? file.withId(sequence++) : file;
            values.put(saved.getId(), saved);
            return saved;
        }
    }

    private static final class RecordingStorage implements FileStoragePort {
        private byte[] lastContent;

        @Override
        public StoredFile store(UserProfileImageUpdateRequest image) {
            lastContent = image.content();
            return new StoredFile(
                    "profiles/profile-image.png",
                    "http://localhost:8080/files/profile-image.png"
            );
        }
    }
}
