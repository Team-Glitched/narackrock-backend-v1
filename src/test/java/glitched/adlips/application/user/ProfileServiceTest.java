package glitched.adlips.application.user;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import glitched.adlips.application.user.port.out.FileStoragePort;
import glitched.adlips.application.user.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.port.out.ProfileLinkPort;
import glitched.adlips.application.user.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.port.out.TransactionPort;
import glitched.adlips.application.user.port.out.UserRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfileServiceTest {

    private InMemoryUsers users;
    private InMemoryProfiles profiles;
    private InMemoryMedia media;
    private RecordingStorage storage;
    private ProfileService service;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        profiles = new InMemoryProfiles();
        media = new InMemoryMedia();
        storage = new RecordingStorage();
        FollowRepositoryPort follows = (followerId, followingId) -> false;
        ProfileLinkPort links = userId -> "https://app.example.com/users/profiles/" + userId;
        TransactionPort transactions = new TransactionPort() {
            @Override
            public <T> T required(Supplier<T> operation) {
                return operation.get();
            }
        };
        Clock clock = Clock.fixed(Instant.parse("2026-06-30T00:00:00Z"), ZoneOffset.UTC);
        service = new ProfileService(
                users, profiles, media, follows, storage, links, transactions, clock
        );

        users.save(User.create("user@example.com").withId(1L));
        profiles.save(Profile.create(1L, "guitar_moon"));
    }

    @Test
    void returnsProfileWithEmptyActivitiesUntilActivityPortsAreConnected() {
        ProfileView result = service.getProfile(1L, 1L);

        assertEquals("guitar_moon", result.nickname());
        assertEquals(0, result.relations().followerCount());
        assertEquals(0, result.activities().posts().size());
        assertEquals(0, result.activities().participatedShorts().size());
        assertEquals(0, result.activities().pinnedShorts().size());
    }

    @Test
    void rejectsPrivateProfileForAnotherUser() {
        profiles.save(Profile.restore(1L, "guitar_moon", null, null, null, true, 0, 0, null));

        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> service.getProfile(1L, 2L)
        );

        assertEquals(UserErrorCode.PRIVATE_PROFILE, exception.getErrorCode());
    }

    @Test
    void updatesOnlyProvidedProfileFields() {
        profiles.save(Profile.restore(1L, "guitar_moon", null, "old", "기타", false, 0, 0, null));

        ProfileUpdateResult result = service.updateProfile(
                1L,
                new UpdateProfileCommand("new_nickname", null, "new explanation")
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
                () -> service.updateProfile(1L, new UpdateProfileCommand("taken", null, null))
        );

        assertEquals(UserErrorCode.DUPLICATE_NICKNAME, exception.getErrorCode());
    }

    @Test
    void storesProfileImageAndReturnsLocalUrl() {
        byte[] image = new byte[]{1, 2, 3};

        ProfileImageResult result = service.updateProfileImage(
                1L,
                new ProfileImageCommand("avatar.png", "image/png", image)
        );

        assertEquals("http://localhost:8080/files/profile-image.png", result.profileImageUrl());
        assertArrayEquals(image, storage.lastContent);
        assertEquals(1L, profiles.findByUserId(1L).orElseThrow().getProfileImageFileId());
    }

    @Test
    void rejectsUnsupportedProfileImage() {
        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> service.updateProfileImage(
                        1L,
                        new ProfileImageCommand("avatar.txt", "text/plain", new byte[]{1})
                )
        );

        assertEquals(UserErrorCode.INVALID_IMAGE_FILE, exception.getErrorCode());
    }

    @Test
    void createsProfileShareLinkForExistingUser() {
        ProfileShareResult result = service.shareProfile(1L);

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
        public StoredFile store(ProfileImageCommand image) {
            lastContent = image.content();
            return new StoredFile(
                    "profiles/profile-image.png",
                    "http://localhost:8080/files/profile-image.png"
            );
        }
    }
}
