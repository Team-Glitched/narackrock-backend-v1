package glitched.adlips.application.community;

import static glitched.adlips.application.community.UpdateGalleryUseCaseTest.assertModerationError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryModerationActionType;
import glitched.adlips.domain.community.GalleryRole;
import glitched.adlips.domain.community.Post;
import glitched.adlips.domain.user.User;
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

class ModeratePostDeletionUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    GalleryAuthorityPort authorityPort;
    PostPort postPort;
    GalleryModerationLogPort logPort;
    ModeratePostDeletionUseCase useCase;
    List<String> calls;

    @BeforeEach
    void setUp() {
        authorityPort = mock(GalleryAuthorityPort.class);
        postPort = mock(PostPort.class);
        logPort = mock(GalleryModerationLogPort.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        calls = new ArrayList<>();
        lenient().when(transactionRunner.required(any())).thenAnswer(invocation -> {
            calls.add("tx-begin");
            Object result = invocation.<Supplier<?>>getArgument(0).get();
            calls.add("tx-end");
            return result;
        });
        lenient().when(authorityPort.existsGallery(10L)).thenReturn(true);
        actor(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false));
        actor(new GalleryActor(1L, UserRole.USER, null, false));
        actor(new GalleryActor(9L, UserRole.ADMIN, null, false));
        useCase = new ModeratePostDeletionUseCase(authorityPort, postPort, logPort, transactionRunner, CLOCK);
    }

    @Test
    void 매니저는_갤러리_글을_삭제하고_이력을_남긴다() {
        Post post = post(1L);
        when(postPort.findActivePost(50L, 10L)).thenReturn(Optional.of(post));
        doAnswer(invocation -> calls.add("update")).when(postPort).update(post);
        doAnswer(invocation -> calls.add("log")).when(logPort)
                .save(10L, 3L, GalleryModerationActionType.DELETE_POST, 1L, 50L, "광고");

        useCase.execute(3L, 10L, 50L, "  광고 ");

        assertThat(post.getDeletedAt()).isEqualTo(NOW);
        assertThat(calls).containsExactly("tx-begin", "update", "log", "tx-end");
    }

    @Test
    void 매니저는_어드민이_쓴_글을_삭제할_수_없다() {
        when(postPort.findActivePost(50L, 10L)).thenReturn(Optional.of(post(9L)));

        assertModerationError(() -> useCase.execute(3L, 10L, 50L, "광고"), GalleryModerationErrorCode.FORBIDDEN);
        verify(postPort, never()).update(any());
    }

    @Test
    void 어드민은_어드민이_쓴_글도_삭제할_수_있다() {
        when(authorityPort.findActor(8L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(8L, UserRole.ADMIN, null, false)));
        Post post = post(8L);
        when(postPort.findActivePost(50L, 10L)).thenReturn(Optional.of(post));

        useCase.execute(9L, 10L, 50L, "정리");

        verify(postPort).update(post);
    }

    @Test
    void 탈퇴한_작성자의_글도_삭제할_수_있다() {
        Post post = post(404L);
        when(postPort.findActivePost(50L, 10L)).thenReturn(Optional.of(post));

        useCase.execute(3L, 10L, 50L, "광고");

        verify(postPort).update(post);
    }

    @Test
    void 일반_유저는_FORBIDDEN이다() {
        assertModerationError(() -> useCase.execute(1L, 10L, 50L, "광고"), GalleryModerationErrorCode.FORBIDDEN);
        verify(postPort, never()).findActivePost(any(), any());
    }

    @Test
    void 다른_갤러리의_글이거나_없는_글이면_POST_NOT_FOUND다() {
        when(postPort.findActivePost(50L, 10L)).thenReturn(Optional.empty());

        assertModerationError(() -> useCase.execute(3L, 10L, 50L, "광고"), GalleryModerationErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 사유가_없으면_VALIDATION_ERROR다() {
        assertModerationError(() -> useCase.execute(3L, 10L, 50L, " "), GalleryModerationErrorCode.VALIDATION_ERROR);
    }

    @Test
    void 갤러리가_없으면_GALLERY_NOT_FOUND다() {
        assertModerationError(() -> useCase.execute(3L, 404L, 50L, "광고"), GalleryModerationErrorCode.GALLERY_NOT_FOUND);
    }

    private void actor(GalleryActor actor) {
        lenient().when(authorityPort.findActor(actor.userId(), 10L, NOW)).thenReturn(Optional.of(actor));
    }

    private static Post post(Long authorId) {
        return new Post(Gallery.create("재즈", "설명"), User.create("a@test.com").withId(authorId), "제목", "내용");
    }
}
