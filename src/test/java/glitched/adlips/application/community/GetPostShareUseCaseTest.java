package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.PostShareLinkPort;
import glitched.adlips.application.community.port.out.PostSharePort;
import org.junit.jupiter.api.Test;

class GetPostShareUseCaseTest {

    final PostSharePort sharePort = mock(PostSharePort.class);
    final PostShareLinkPort linkPort = mock(PostShareLinkPort.class);
    final GetPostShareUseCase useCase = new GetPostShareUseCase(sharePort, linkPort);

    @Test
    void 삭제되지_않은_게시글이면_공유_링크를_반환한다() {
        when(sharePort.existsActivePost(501L)).thenReturn(true);
        when(linkPort.create(501L)).thenReturn("https://app.example.com/posts/501");

        assertThat(useCase.execute(501L)).isEqualTo(new PostShareResult(501L, "https://app.example.com/posts/501"));
    }

    @Test
    void 없거나_삭제된_게시글이면_POST_NOT_FOUND() {
        when(sharePort.existsActivePost(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(999L))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode").isEqualTo(PostErrorCode.POST_NOT_FOUND);
        verifyNoInteractions(linkPort);
    }
}
