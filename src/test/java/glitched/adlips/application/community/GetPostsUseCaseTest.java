package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.PostQueryItem;
import glitched.adlips.application.community.port.out.PostQueryPort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetPostsUseCaseTest {

    PostQueryPort port;
    GetPostsUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(PostQueryPort.class);
        useCase = new GetPostsUseCase(port);
    }

    @Test
    void 정상_조회시_갤러리ID와_총개수와_목록을_반환한다() {
        PostQueryItem item = new PostQueryItem(
                501L, 10L, 1L, "writer", null, "제목", "내용", 0, 0, 0, 0, LocalDateTime.of(2026, 8, 23, 10, 0));
        when(port.existsGallery(10L)).thenReturn(true);
        when(port.findGalleryName(10L)).thenReturn(java.util.Optional.of("자유 갤러리"));
        when(port.findPosts(10L, null, PostSort.LATEST, 0, 20)).thenReturn(List.of(item));
        when(port.countPosts(10L, null)).thenReturn(1L);

        PostListResult result = useCase.execute(10L, 0, 20, null, "LATEST");

        assertThat(result.galleryId()).isEqualTo(10L);
        assertThat(result.galleryName()).isEqualTo("자유 갤러리");
        assertThat(result.currentSort()).isEqualTo(PostSort.LATEST);
        assertThat(result.searchedKeyword()).isNull();
        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.posts()).containsExactly(item);
    }

    @Test
    void 검색어와_정렬조건을_포트에_전달한다() {
        when(port.existsGallery(10L)).thenReturn(true);
        when(port.findGalleryName(10L)).thenReturn(java.util.Optional.of("자유 갤러리"));
        when(port.findPosts(10L, "기타", PostSort.POPULAR, 0, 20)).thenReturn(List.of());
        when(port.countPosts(10L, "기타")).thenReturn(0L);

        PostListResult result = useCase.execute(10L, 0, 20, "  기타  ", "POPULAR");

        assertThat(result.currentSort()).isEqualTo(PostSort.POPULAR);
        assertThat(result.searchedKeyword()).isEqualTo("기타");
        org.mockito.Mockito.verify(port).findPosts(10L, "기타", PostSort.POPULAR, 0, 20);
        org.mockito.Mockito.verify(port).countPosts(10L, "기타");
    }

    @Test
    void 존재하지_않는_갤러리면_GALLERY_NOT_FOUND가_발생한다() {
        when(port.existsGallery(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(999L, 0, 20, null, "LATEST"))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.GALLERY_NOT_FOUND);
    }

    @Test
    void 페이지가_음수면_VALIDATION_ERROR가_발생한다() {
        assertThatThrownBy(() -> useCase.execute(10L, -1, 20, null, "LATEST"))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.VALIDATION_ERROR);
    }

    @Test
    void 사이즈가_0이하면_VALIDATION_ERROR가_발생한다() {
        assertThatThrownBy(() -> useCase.execute(10L, 0, 0, null, "LATEST"))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.VALIDATION_ERROR);
    }

    @Test
    void 사이즈가_최대치를_초과하면_VALIDATION_ERROR가_발생한다() {
        assertThatThrownBy(() -> useCase.execute(10L, 0, 101, null, "LATEST"))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.VALIDATION_ERROR);
    }

    @Test
    void 공백만_있는_검색어면_INVALID_SEARCH_KEYWORD가_발생한다() {
        assertThatThrownBy(() -> useCase.execute(10L, 0, 20, "   ", "LATEST"))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.INVALID_SEARCH_KEYWORD);
    }

    @Test
    void 잘못된_정렬조건이면_INVALID_SORT_CONDITION이_발생한다() {
        assertThatThrownBy(() -> useCase.execute(10L, 0, 20, null, "TRENDING"))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.INVALID_SORT_CONDITION);
    }
}
