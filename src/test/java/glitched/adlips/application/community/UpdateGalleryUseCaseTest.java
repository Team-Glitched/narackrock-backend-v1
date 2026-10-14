package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateGalleryUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    GalleryAuthorityPort authorityPort;
    GalleryManagementPort managementPort;
    GalleryModerationLogPort logPort;
    TransactionRunner transactionRunner;
    UpdateGalleryUseCase useCase;
    List<String> calls;
    Gallery gallery;

    @BeforeEach
    void setUp() {
        authorityPort = mock(GalleryAuthorityPort.class);
        managementPort = mock(GalleryManagementPort.class);
        logPort = mock(GalleryModerationLogPort.class);
        transactionRunner = mock(TransactionRunner.class);
        calls = new ArrayList<>();
        lenient().when(transactionRunner.required(any())).thenAnswer(invocation -> {
            calls.add("tx-begin");
            Object result = invocation.<Supplier<?>>getArgument(0).get();
            calls.add("tx-end");
            return result;
        });
        gallery = Gallery.create("재즈", "재즈 이야기");
        lenient().when(managementPort.findGalleryForUpdate(10L)).thenReturn(Optional.of(gallery));
        useCase = new UpdateGalleryUseCase(authorityPort, managementPort, logPort, transactionRunner, CLOCK);
    }

    @Test
    void 매니저는_소개를_바꾸고_이력을_같은_트랜잭션에서_남긴다() {
        actor(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false));
        doAnswer(invocation -> calls.add("update")).when(managementPort).updateGallery(gallery);
        doAnswer(invocation -> calls.add("log")).when(logPort)
                .save(10L, 3L, GalleryModerationActionType.UPDATE_GALLERY, null, 10L, null);

        useCase.execute(3L, 10L, "  새 소개  ");

        assertThat(gallery.getDescription()).isEqualTo("새 소개");
        assertThat(calls).containsExactly("tx-begin", "update", "log", "tx-end");
    }

    @Test
    void 어드민도_소개를_바꿀_수_있다() {
        actor(new GalleryActor(9L, UserRole.ADMIN, null, false));

        useCase.execute(9L, 10L, "새 소개");

        verify(managementPort).updateGallery(gallery);
    }

    @Test
    void 일반_유저는_FORBIDDEN이다() {
        actor(new GalleryActor(1L, UserRole.USER, null, false));

        assertModerationError(() -> useCase.execute(1L, 10L, "새 소개"), GalleryModerationErrorCode.FORBIDDEN);
        verify(managementPort, never()).updateGallery(any());
        assertThat(gallery.getDescription()).isEqualTo("재즈 이야기");
    }

    @Test
    void 다른_갤러리의_매니저는_이_갤러리에서_역할이_없으므로_FORBIDDEN이다() {
        actor(new GalleryActor(4L, UserRole.USER, null, false));

        assertModerationError(() -> useCase.execute(4L, 10L, "새 소개"), GalleryModerationErrorCode.FORBIDDEN);
    }

    @Test
    void 전역_정지된_매니저는_FORBIDDEN이다() {
        actor(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, true));

        assertModerationError(() -> useCase.execute(3L, 10L, "새 소개"), GalleryModerationErrorCode.FORBIDDEN);
    }

    @Test
    void 사용자를_찾을_수_없으면_FORBIDDEN이다() {
        when(authorityPort.findActor(5L, 10L, NOW)).thenReturn(Optional.empty());

        assertModerationError(() -> useCase.execute(5L, 10L, "새 소개"), GalleryModerationErrorCode.FORBIDDEN);
    }

    @Test
    void 갤러리가_없으면_GALLERY_NOT_FOUND다() {
        when(managementPort.findGalleryForUpdate(404L)).thenReturn(Optional.empty());

        assertModerationError(() -> useCase.execute(3L, 404L, "새 소개"), GalleryModerationErrorCode.GALLERY_NOT_FOUND);
    }

    @Test
    void 소개가_규칙을_어기면_VALIDATION_ERROR다() {
        actor(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false));

        assertModerationError(() -> useCase.execute(3L, 10L, " "), GalleryModerationErrorCode.VALIDATION_ERROR);
        verify(logPort, never()).save(any(), any(), any(), any(), any(), any());
    }

    private void actor(GalleryActor actor) {
        when(authorityPort.findActor(actor.userId(), 10L, NOW)).thenReturn(Optional.of(actor));
    }

    static void assertModerationError(Runnable action, GalleryModerationErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(GalleryModerationApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
