package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryBanQueryPort;
import glitched.adlips.application.community.port.out.GallerySubscriptionPort;
import glitched.adlips.application.community.port.out.PostCommentPort;
import glitched.adlips.application.community.port.out.PostCommentReactionPort;
import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.community.port.out.PostReactionPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.Post;
import glitched.adlips.domain.community.PostComment;
import glitched.adlips.domain.reaction.ReactionType;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 갤러리 이용정지·추방이 게시글·댓글·반응·구독 유스케이스에서 집행되는지 확인한다. */
class GalleryRestrictionEnforcementTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final TransactionRunner TX = TransactionRunner.direct();
    private static final Long USER = 1L;

    GalleryBanQueryPort galleryBanQueryPort;

    @BeforeEach
    void setUp() {
        galleryBanQueryPort = mock(GalleryBanQueryPort.class);
    }

    @Test
    void 제재된_사용자는_그_갤러리에_게시글을_쓸_수_없다() {
        PostPort postPort = mock(PostPort.class);
        when(postPort.existsGallery(10L)).thenReturn(true);
        GallerySubscriptionPort subscriptions = mock(GallerySubscriptionPort.class);
        when(subscriptions.lockGallery(10L)).thenReturn(true);
        when(subscriptions.isSubscribed(USER, 10L)).thenReturn(true);
        when(galleryBanQueryPort.isRestrictedInGallery(10L, USER, NOW)).thenReturn(true);
        CreatePostUseCase useCase = new CreatePostUseCase(
                postPort, subscriptions, (userId, now) -> false, galleryBanQueryPort, mock(MediaFileRepositoryPort.class), TX, CLOCK);

        assertThatThrownBy(() -> useCase.execute(10L, USER, "제목", "내용", null))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode").isEqualTo(PostErrorCode.GALLERY_RESTRICTED_USER);
        verify(postPort, never()).save(anyLong(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void 제재되지_않았으면_게시글을_쓸_수_있다() {
        PostPort postPort = mock(PostPort.class);
        when(postPort.existsGallery(10L)).thenReturn(true);
        GallerySubscriptionPort subscriptions = mock(GallerySubscriptionPort.class);
        when(subscriptions.lockGallery(10L)).thenReturn(true);
        when(subscriptions.isSubscribed(USER, 10L)).thenReturn(true);
        CreatePostUseCase useCase = new CreatePostUseCase(
                postPort, subscriptions, (userId, now) -> false, galleryBanQueryPort, mock(MediaFileRepositoryPort.class), TX, CLOCK);

        useCase.execute(10L, USER, "제목", "내용", null);

        verify(postPort).save(10L, USER, "제목", "내용", null);
    }

    @Test
    void 제재된_사용자는_그_갤러리의_자기_글을_수정할_수_없다() {
        PostPort postPort = mock(PostPort.class);
        Post post = mock(Post.class);
        when(post.getUserId()).thenReturn(USER);
        when(post.getGalleryId()).thenReturn(10L);
        when(postPort.findActivePost(5L)).thenReturn(Optional.of(post));
        when(galleryBanQueryPort.isRestrictedInGallery(10L, USER, NOW)).thenReturn(true);
        UpdatePostUseCase useCase = new UpdatePostUseCase(
                postPort, (userId, now) -> false, galleryBanQueryPort, TX, CLOCK);

        assertThatThrownBy(() -> useCase.execute(5L, USER, "제목", "내용"))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode").isEqualTo(PostErrorCode.GALLERY_RESTRICTED_USER);
        verify(postPort, never()).update(any());
    }

    @Test
    void 제재된_사용자는_그_갤러리_글에_댓글을_쓸_수_없다() {
        PostCommentPort commentPort = mock(PostCommentPort.class);
        when(commentPort.existsActivePost(5L)).thenReturn(true);
        when(galleryBanQueryPort.isRestrictedForPost(5L, USER, NOW)).thenReturn(true);
        CreatePostCommentUseCase useCase = new CreatePostCommentUseCase(
                commentPort, (userId, now) -> false, galleryBanQueryPort, TX, CLOCK);

        assertThatThrownBy(() -> useCase.execute(5L, USER, "댓글", null))
                .isInstanceOf(PostCommentApplicationException.class)
                .extracting("errorCode").isEqualTo(PostCommentErrorCode.GALLERY_RESTRICTED_USER);
        verify(commentPort, never()).save(anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void 제재된_사용자는_그_갤러리의_자기_댓글을_수정할_수_없다() {
        PostCommentPort commentPort = mock(PostCommentPort.class);
        PostComment comment = mock(PostComment.class);
        when(comment.getUserId()).thenReturn(USER);
        when(commentPort.findActiveComment(7L)).thenReturn(Optional.of(comment));
        when(galleryBanQueryPort.isRestrictedForComment(7L, USER, NOW)).thenReturn(true);
        UpdatePostCommentUseCase useCase = new UpdatePostCommentUseCase(
                commentPort, (userId, now) -> false, galleryBanQueryPort, TX, CLOCK);

        assertThatThrownBy(() -> useCase.execute(7L, USER, "수정"))
                .isInstanceOf(PostCommentApplicationException.class)
                .extracting("errorCode").isEqualTo(PostCommentErrorCode.GALLERY_RESTRICTED_USER);
        verify(commentPort, never()).update(any());
    }

    @Test
    void 제재된_사용자는_그_갤러리_글에_반응할_수_없다() {
        PostReactionPort reactionPort = mock(PostReactionPort.class);
        when(reactionPort.lockActivePost(5L)).thenReturn(true);
        when(galleryBanQueryPort.isRestrictedForPost(5L, USER, NOW)).thenReturn(true);
        TogglePostReactionUseCase useCase = new TogglePostReactionUseCase(
                reactionPort, (userId, now) -> false, galleryBanQueryPort, TX, CLOCK);

        assertThatThrownBy(() -> useCase.toggle(USER, 5L, ReactionType.LIKE))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode").isEqualTo(PostErrorCode.GALLERY_RESTRICTED_USER);
        verify(reactionPort, never()).adjustReactionCounts(anyLong(), anyInt(), anyInt());
    }

    @Test
    void 제재된_사용자는_그_갤러리_댓글에_반응할_수_없다() {
        PostCommentReactionPort reactionPort = mock(PostCommentReactionPort.class);
        when(reactionPort.lockActiveComment(7L)).thenReturn(true);
        when(galleryBanQueryPort.isRestrictedForComment(7L, USER, NOW)).thenReturn(true);
        TogglePostCommentReactionUseCase useCase = new TogglePostCommentReactionUseCase(
                reactionPort, (userId, now) -> false, galleryBanQueryPort, TX, CLOCK);

        assertThatThrownBy(() -> useCase.toggle(USER, 7L, ReactionType.LIKE))
                .isInstanceOf(PostCommentApplicationException.class)
                .extracting("errorCode").isEqualTo(PostCommentErrorCode.GALLERY_RESTRICTED_USER);
        verify(reactionPort, never()).adjustReactionCounts(anyLong(), anyInt(), anyInt());
    }

    @Test
    void 추방된_사용자는_다시_구독할_수_없다() {
        GallerySubscriptionPort subscriptionPort = mock(GallerySubscriptionPort.class);
        when(subscriptionPort.lockGallery(10L)).thenReturn(true);
        when(galleryBanQueryPort.isExpelled(10L, USER, NOW)).thenReturn(true);
        SubscribeGalleryUseCase useCase = new SubscribeGalleryUseCase(subscriptionPort, galleryBanQueryPort, TX, CLOCK);

        assertThatThrownBy(() -> useCase.execute(USER, 10L))
                .isInstanceOf(GalleryApplicationException.class)
                .extracting("errorCode").isEqualTo(GalleryErrorCode.EXPELLED_FROM_GALLERY);
        verify(subscriptionPort, never()).saveSubscription(anyLong(), anyLong());
    }

    @Test
    void 이용정지만_된_사용자는_구독할_수_있다() {
        GallerySubscriptionPort subscriptionPort = mock(GallerySubscriptionPort.class);
        when(subscriptionPort.lockGallery(10L)).thenReturn(true);
        when(galleryBanQueryPort.isRestrictedInGallery(10L, USER, NOW)).thenReturn(true);
        SubscribeGalleryUseCase useCase = new SubscribeGalleryUseCase(subscriptionPort, galleryBanQueryPort, TX, CLOCK);

        useCase.execute(USER, 10L);

        verify(subscriptionPort).saveSubscription(USER, 10L);
    }

    @Test
    void 제재_검사가_없는_기존_생성자는_제재를_무시한다() {
        PostPort postPort = mock(PostPort.class);
        when(postPort.existsGallery(10L)).thenReturn(true);
        GallerySubscriptionPort subscriptions = mock(GallerySubscriptionPort.class);
        when(subscriptions.lockGallery(10L)).thenReturn(true);
        when(subscriptions.isSubscribed(USER, 10L)).thenReturn(true);
        Post post = new Post(Gallery.create("재즈", "설명"), User.create("a@test.com").withId(USER), "제목", "내용");
        when(postPort.findActivePost(5L)).thenReturn(Optional.of(post));

        new UpdatePostUseCase(postPort, TX).execute(5L, USER, "새 제목", "새 내용");

        verify(postPort).update(post);
    }
}
