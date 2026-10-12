package glitched.adlips.application.community;

import static glitched.adlips.application.community.UpdateGalleryUseCaseTest.assertModerationError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryBanPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryBan;
import glitched.adlips.domain.community.GalleryModerationActionType;
import glitched.adlips.domain.community.GalleryRole;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LiftGalleryBanUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    GalleryAuthorityPort authorityPort;
    GalleryManagementPort managementPort;
    GalleryBanPort banPort;
    UserRepositoryPort userRepository;
    GalleryModerationLogPort logPort;
    LiftGalleryBanUseCase useCase;
    Gallery gallery;
    User manager = User.restore(3L, "m@test.com", UserRole.USER, null);
    User target = User.restore(1L, "u@test.com", UserRole.USER, null);

    @BeforeEach
    void setUp() {
        authorityPort = mock(GalleryAuthorityPort.class);
        managementPort = mock(GalleryManagementPort.class);
        banPort = mock(GalleryBanPort.class);
        userRepository = mock(UserRepositoryPort.class);
        logPort = mock(GalleryModerationLogPort.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        gallery = Gallery.create("재즈", "설명");
        ReflectionTestUtils.setField(gallery, "id", 10L);
        lenient().when(managementPort.findGalleryForUpdate(10L)).thenReturn(Optional.of(gallery));
        lenient().when(authorityPort.findActor(3L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false)));
        lenient().when(userRepository.findById(3L)).thenReturn(Optional.of(manager));
        useCase = new LiftGalleryBanUseCase(
                authorityPort, managementPort, banPort, userRepository, logPort, transactionRunner, CLOCK);
    }

    @Test
    void 활성_제재를_해제하고_이력을_남긴다() {
        GalleryBan ban = GalleryBan.expel(gallery, target, manager, "광고");
        when(banPort.findById(70L)).thenReturn(Optional.of(ban));

        useCase.execute(3L, 10L, 70L);

        assertThat(ban.getLiftedAt()).isEqualTo(NOW);
        assertThat(ban.getLiftedById()).isEqualTo(3L);
        verify(banPort).update(ban);
        verify(logPort).save(10L, 3L, GalleryModerationActionType.LIFT_BAN, 1L, 70L, null);
    }

    @Test
    void 일반_유저는_FORBIDDEN이다() {
        when(authorityPort.findActor(2L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(2L, UserRole.USER, null, false)));

        assertModerationError(() -> useCase.execute(2L, 10L, 70L), GalleryModerationErrorCode.FORBIDDEN);
        verify(banPort, never()).update(any());
    }

    @Test
    void 다른_갤러리의_제재는_GALLERY_BAN_NOT_FOUND다() {
        Gallery other = Gallery.create("락앤롤", "설명");
        ReflectionTestUtils.setField(other, "id", 11L);
        when(banPort.findById(70L)).thenReturn(Optional.of(GalleryBan.expel(other, target, manager, "광고")));

        assertModerationError(() -> useCase.execute(3L, 10L, 70L), GalleryModerationErrorCode.GALLERY_BAN_NOT_FOUND);
    }

    @Test
    void 이미_해제되었거나_만료된_제재는_GALLERY_BAN_NOT_FOUND다() {
        GalleryBan lifted = GalleryBan.expel(gallery, target, manager, "광고");
        lifted.lift(manager, NOW.minusDays(1));
        GalleryBan expired = GalleryBan.suspend(gallery, target, manager, "도배", 1, NOW.minusDays(2));
        when(banPort.findById(70L)).thenReturn(Optional.of(lifted));
        when(banPort.findById(71L)).thenReturn(Optional.of(expired));

        assertModerationError(() -> useCase.execute(3L, 10L, 70L), GalleryModerationErrorCode.GALLERY_BAN_NOT_FOUND);
        assertModerationError(() -> useCase.execute(3L, 10L, 71L), GalleryModerationErrorCode.GALLERY_BAN_NOT_FOUND);
        verify(banPort, never()).update(any());
    }

    @Test
    void 갤러리가_없으면_GALLERY_NOT_FOUND다() {
        assertModerationError(() -> useCase.execute(3L, 404L, 70L), GalleryModerationErrorCode.GALLERY_NOT_FOUND);
    }
}
