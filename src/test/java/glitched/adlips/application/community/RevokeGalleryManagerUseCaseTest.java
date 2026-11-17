package glitched.adlips.application.community;

import static glitched.adlips.application.community.UpdateGalleryUseCaseTest.assertModerationError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryModerationActionType;
import glitched.adlips.domain.community.GalleryRole;
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

class RevokeGalleryManagerUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    GalleryAuthorityPort authorityPort;
    GalleryManagementPort managementPort;
    GalleryModerationLogPort logPort;
    RevokeGalleryManagerUseCase useCase;

    @BeforeEach
    void setUp() {
        authorityPort = mock(GalleryAuthorityPort.class);
        managementPort = mock(GalleryManagementPort.class);
        logPort = mock(GalleryModerationLogPort.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        Gallery gallery = Gallery.create("재즈", "설명");
        ReflectionTestUtils.setField(gallery, "id", 10L);
        lenient().when(managementPort.findGalleryForUpdate(10L)).thenReturn(Optional.of(gallery));
        actor(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false));
        actor(new GalleryActor(4L, UserRole.USER, GalleryRole.MANAGER, false));
        actor(new GalleryActor(9L, UserRole.ADMIN, null, false));
        actor(new GalleryActor(1L, UserRole.USER, null, false));
        useCase = new RevokeGalleryManagerUseCase(authorityPort, managementPort, logPort, transactionRunner, CLOCK);
    }

    private void actor(GalleryActor actor) {
        lenient().when(authorityPort.findActor(actor.userId(), 10L, NOW)).thenReturn(Optional.of(actor));
    }

    @Test
    void 커뮤니티_어드민이_다른_어드민을_해임하고_이력을_남긴다() {
        when(managementPort.countActiveManagers(10L)).thenReturn(2L);

        useCase.execute(3L, 10L, 4L);

        verify(managementPort).removeManager(10L, 4L);
        verify(logPort).save(10L, 3L, GalleryModerationActionType.REVOKE_MANAGER, 4L, null, null);
    }

    @Test
    void 마지막_어드민은_스스로_해임할_수_없다() {
        when(managementPort.countActiveManagers(10L)).thenReturn(1L);

        assertModerationError(() -> useCase.execute(3L, 10L, 3L), GalleryModerationErrorCode.LAST_GALLERY_MANAGER);
        verify(managementPort, never()).removeManager(anyLong(), anyLong());
    }

    @Test
    void 운영자_어드민은_마지막_어드민도_해임할_수_있다() {
        when(managementPort.countActiveManagers(10L)).thenReturn(1L);

        useCase.execute(9L, 10L, 3L);

        verify(managementPort).removeManager(10L, 3L);
        verify(logPort).save(10L, 9L, GalleryModerationActionType.REVOKE_MANAGER, 3L, null, null);
    }

    @Test
    void 대상이_어드민이_아니면_GALLERY_MANAGER_NOT_FOUND다() {
        assertModerationError(() -> useCase.execute(3L, 10L, 1L), GalleryModerationErrorCode.GALLERY_MANAGER_NOT_FOUND);
        assertModerationError(() -> useCase.execute(3L, 10L, 404L), GalleryModerationErrorCode.GALLERY_MANAGER_NOT_FOUND);
        verify(managementPort, never()).removeManager(anyLong(), anyLong());
    }

    @Test
    void 권한이_없으면_FORBIDDEN이다() {
        assertModerationError(() -> useCase.execute(1L, 10L, 3L), GalleryModerationErrorCode.FORBIDDEN);
        verify(managementPort, never()).removeManager(anyLong(), anyLong());
    }
}
