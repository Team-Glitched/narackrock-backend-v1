package glitched.adlips.application.community;

import static glitched.adlips.application.community.UpdateGalleryUseCaseTest.assertModerationError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.community.port.out.PostCommentPort;
import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryModerationActionType;
import glitched.adlips.domain.community.GalleryRole;
import glitched.adlips.domain.community.Post;
import glitched.adlips.domain.community.PostComment;
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

class ModeratePostCommentDeletionUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    GalleryAuthorityPort authorityPort;
    PostPort postPort;
    PostCommentPort commentPort;
    GalleryModerationLogPort logPort;
    ModeratePostCommentDeletionUseCase useCase;
    Post post;

    @BeforeEach
    void setUp() {
        authorityPort = mock(GalleryAuthorityPort.class);
        postPort = mock(PostPort.class);
        commentPort = mock(PostCommentPort.class);
        logPort = mock(GalleryModerationLogPort.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        lenient().when(authorityPort.existsGallery(10L)).thenReturn(true);
        actor(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false));
        actor(new GalleryActor(1L, UserRole.USER, null, false));
        actor(new GalleryActor(9L, UserRole.ADMIN, null, false));
        post = new Post(Gallery.create("재즈", "설명"), User.create("w@test.com").withId(2L), "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 50L);
        lenient().when(postPort.findActivePost(50L, 10L)).thenReturn(Optional.of(post));
        useCase = new ModeratePostCommentDeletionUseCase(
                authorityPort, postPort, commentPort, logPort, transactionRunner, CLOCK);
    }

    @Test
    void 최상위_댓글을_지우면_답글도_지우고_댓글_수를_줄인다() {
        PostComment comment = comment(70L, 1L, null);
        when(commentPort.findActiveComment(70L)).thenReturn(Optional.of(comment));
        when(commentPort.softDeleteActiveReplies(70L, NOW)).thenReturn(2);

        useCase.execute(3L, 10L, 70L, "욕설");

        assertThat(comment.getDeletedAt()).isEqualTo(NOW);
        verify(commentPort).update(comment);
        verify(commentPort).adjustPostCommentCount(50L, -3);
        verify(commentPort, never()).adjustParentReplyCount(anyLong(), anyInt());
        verify(logPort).save(10L, 3L, GalleryModerationActionType.DELETE_COMMENT, 1L, 70L, "욕설");
    }

    @Test
    void 답글을_지우면_부모의_답글_수를_줄인다() {
        PostComment reply = comment(71L, 1L, comment(70L, 2L, null));
        when(commentPort.findActiveComment(71L)).thenReturn(Optional.of(reply));

        useCase.execute(3L, 10L, 71L, "욕설");

        verify(commentPort).adjustPostCommentCount(50L, -1);
        verify(commentPort).adjustParentReplyCount(70L, -1);
        verify(commentPort, never()).softDeleteActiveReplies(anyLong(), any());
    }

    @Test
    void 매니저는_어드민이_쓴_댓글을_삭제할_수_없다() {
        when(commentPort.findActiveComment(70L)).thenReturn(Optional.of(comment(70L, 9L, null)));

        assertModerationError(() -> useCase.execute(3L, 10L, 70L, "욕설"), GalleryModerationErrorCode.FORBIDDEN);
        verify(commentPort, never()).update(any());
    }

    @Test
    void 일반_유저는_FORBIDDEN이다() {
        assertModerationError(() -> useCase.execute(1L, 10L, 70L, "욕설"), GalleryModerationErrorCode.FORBIDDEN);
        verify(commentPort, never()).findActiveComment(anyLong());
    }

    @Test
    void 다른_갤러리의_댓글이거나_없는_댓글이면_COMMENT_NOT_FOUND다() {
        PostComment other = comment(72L, 1L, null);
        ReflectionTestUtils.setField(other, "post", otherGalleryPost());
        when(commentPort.findActiveComment(72L)).thenReturn(Optional.of(other));
        when(postPort.findActivePost(51L, 10L)).thenReturn(Optional.empty());

        assertModerationError(() -> useCase.execute(3L, 10L, 72L, "욕설"), GalleryModerationErrorCode.COMMENT_NOT_FOUND);
        assertModerationError(() -> useCase.execute(3L, 10L, 404L, "욕설"),
                GalleryModerationErrorCode.COMMENT_NOT_FOUND);
        verify(commentPort, never()).update(any());
    }

    @Test
    void 사유가_없으면_VALIDATION_ERROR다() {
        assertModerationError(() -> useCase.execute(3L, 10L, 70L, null), GalleryModerationErrorCode.VALIDATION_ERROR);
    }

    private void actor(GalleryActor actor) {
        lenient().when(authorityPort.findActor(actor.userId(), 10L, NOW)).thenReturn(Optional.of(actor));
    }

    private PostComment comment(Long id, Long authorId, PostComment parent) {
        PostComment comment = new PostComment(post, User.create("c@test.com").withId(authorId), "댓글", parent);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private static Post otherGalleryPost() {
        Post other = new Post(Gallery.create("락앤롤", "설명"), User.create("o@test.com").withId(2L), "제목", "내용");
        ReflectionTestUtils.setField(other, "id", 51L);
        return other;
    }
}
