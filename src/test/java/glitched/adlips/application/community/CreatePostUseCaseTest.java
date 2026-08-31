package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreatePostUseCaseTest {

    PostPort port;
    TransactionRunner transactionRunner;
    UserBanQueryPort userBanQueryPort;
    CreatePostUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(PostPort.class);
        transactionRunner = mock(TransactionRunner.class);
        userBanQueryPort = mock(UserBanQueryPort.class);
        when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new CreatePostUseCase(port, transactionRunner);
    }

    @Test
    void 정상_게시글_생성시_postId와_galleryId를_반환한다() {
        when(port.existsGallery(10L)).thenReturn(true);
        when(port.save(10L, 1L, "제목", "내용")).thenReturn(501L);

        PostResult result = useCase.execute(10L, 1L, "제목", "내용");

        assertThat(result.postId()).isEqualTo(501L);
        assertThat(result.galleryId()).isEqualTo(10L);
    }

    @Test
    void 제목이_공백이면_VALIDATION_ERROR_예외가_발생하고_포트를_호출하지_않는다() {
        assertThatThrownBy(() -> useCase.execute(10L, 1L, "  ", "내용"))
                .isInstanceOf(PostApplicationException.class)
                .hasMessage("제목은 필수 입력 사항입니다.")
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.VALIDATION_ERROR);

        verify(port, never()).existsGallery(any());
    }

    @Test
    void 내용이_공백이면_VALIDATION_ERROR_예외가_발생한다() {
        assertThatThrownBy(() -> useCase.execute(10L, 1L, "제목", "  "))
                .isInstanceOf(PostApplicationException.class)
                .hasMessage("내용은 필수 입력 사항입니다.")
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.VALIDATION_ERROR);
    }

    @Test
    void 존재하지_않는_갤러리면_GALLERY_NOT_FOUND_예외가_발생하고_저장하지_않는다() {
        when(port.existsGallery(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(999L, 1L, "제목", "내용"))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.GALLERY_NOT_FOUND);

        verify(port, never()).save(any(), any(), any(), any());
    }

    @Test
    void 정지된_사용자는_BANNED_USER_ACCESS_예외가_발생하고_저장하지_않는다() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-23T10:00:00Z"), ZoneOffset.UTC);
        when(userBanQueryPort.isBanned(1L, java.time.LocalDateTime.of(2026, 8, 23, 10, 0)))
                .thenReturn(true);
        useCase = new CreatePostUseCase(port, userBanQueryPort, transactionRunner, clock);

        assertThatThrownBy(() -> useCase.execute(10L, 1L, "제목", "내용"))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.BANNED_USER_ACCESS);
        verify(port, never()).save(any(), any(), any(), any());
    }
}
