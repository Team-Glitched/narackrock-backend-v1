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
import glitched.adlips.application.community.port.out.GalleryBanPort;
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

class GrantGalleryManagerUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    GalleryAuthorityPort authorityPort;
    GalleryManagementPort managementPort;
    GalleryBanPort banPort;
    GalleryModerationLogPort logPort;
    GrantGalleryManagerUseCase useCase;

    @BeforeEach
    void setUp() {
        authorityPort = mock(GalleryAuthorityPort.class);
        managementPort = mock(GalleryManagementPort.class);
        banPort = mock(GalleryBanPort.class);
        logPort = mock(GalleryModerationLogPort.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        Gallery gallery = Gallery.create("재즈", "설명");
        ReflectionTestUtils.setField(gallery, "id", 10L);
        lenient().when(managementPort.findGalleryForUpdate(10L)).thenReturn(Optional.of(gallery));
        actor(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false));
        actor(new GalleryActor(9L, UserRole.ADMIN, null, false));
        actor(new GalleryActor(1L, UserRole.USER, null, false));
        useCase = new GrantGalleryManagerUseCase(
                authorityPort, managementPort, banPort, logPort, transactionRunner, CLOCK);
    }

    private void actor(GalleryActor actor) {
        lenient().when(authorityPort.findActor(actor.userId(), 10L, NOW)).thenReturn(Optional.of(actor));
    }

    @Test
    void 커뮤니티_어드민이_지정하면_어드민이_되고_이력이_남는다() {
        useCase.execute(3L, 10L, 1L);

        verify(managementPort).addManager(10L, 1L, GalleryRole.MANAGER);
        verify(logPort).save(10L, 3L, GalleryModerationActionType.GRANT_MANAGER, 1L, null, null);
    }

    @Test
    void 운영자_어드민도_지정할_수_있다() {
        useCase.execute(9L, 10L, 1L);

        verify(managementPort).addManager(10L, 1L, GalleryRole.MANAGER);
        verify(logPort).save(10L, 9L, GalleryModerationActionType.GRANT_MANAGER, 1L, null, null);
    }

    @Test
    void 권한이_없으면_FORBIDDEN이다() {
        actor(new GalleryActor(2L, UserRole.USER, null, false));

        assertModerationError(() -> useCase.execute(2L, 10L, 1L), GalleryModerationErrorCode.FORBIDDEN);
        verify(managementPort, never()).addManager(anyLong(), anyLong(), any());
    }

    @Test
    void 이미_어드민이면_ALREADY_GALLERY_MANAGER다() {
        actor(new GalleryActor(4L, UserRole.USER, GalleryRole.MANAGER, false));

        assertModerationError(() -> useCase.execute(3L, 10L, 4L), GalleryModerationErrorCode.ALREADY_GALLERY_MANAGER);
        verify(managementPort, never()).addManager(anyLong(), anyLong(), any());
    }

    @Test
    void 전역_정지_중이면_CANNOT_GRANT_MANAGER다() {
        actor(new GalleryActor(5L, UserRole.USER, null, true));

        assertModerationError(() -> useCase.execute(3L, 10L, 5L), GalleryModerationErrorCode.CANNOT_GRANT_MANAGER);
        verify(managementPort, never()).addManager(anyLong(), anyLong(), any());
    }

    @Test
    void 커뮤니티_제재_중이면_CANNOT_GRANT_MANAGER다() {
        when(banPort.existsActive(10L, 1L, NOW)).thenReturn(true);

        assertModerationError(() -> useCase.execute(3L, 10L, 1L), GalleryModerationErrorCode.CANNOT_GRANT_MANAGER);
        verify(managementPort, never()).addManager(anyLong(), anyLong(), any());
    }

    @Test
    void 대상이_없으면_TARGET_USER_NOT_FOUND다() {
        assertModerationError(() -> useCase.execute(3L, 10L, 404L), GalleryModerationErrorCode.TARGET_USER_NOT_FOUND);
    }

    @Test
    void 갤러리가_없으면_GALLERY_NOT_FOUND다() {
        assertModerationError(() -> useCase.execute(3L, 404L, 1L), GalleryModerationErrorCode.GALLERY_NOT_FOUND);
    }
}
