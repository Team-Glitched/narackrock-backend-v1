package glitched.adlips.adapter.in.web.community;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.GetPopularPostsUseCase;
import glitched.adlips.application.community.PopularPostListResult;
import glitched.adlips.application.community.PostApplicationException;
import glitched.adlips.application.community.PostErrorCode;
import glitched.adlips.application.community.port.out.PostQueryItem;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.global.config.SecurityConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PopularPostController.class)
@Import(SecurityConfig.class)
class PopularPostControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean GetPopularPostsUseCase getPopularPostsUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    @Test
    void 인기글은_인증없이_갤러리_정보와_함께_조회된다() throws Exception {
        PostQueryItem item = new PostQueryItem(
                501L, 10L, "음악", 1L, "writer", null, "제목", "내용", null,
                3, 9, 0, 2, false, false, false, LocalDateTime.of(2026, 9, 27, 10, 0), null);
        when(getPopularPostsUseCase.execute(0, 20)).thenReturn(new PopularPostListResult(0, 20, 1L, List.of(item)));

        mockMvc.perform(get("/api/v1/posts/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.posts[0].postId").value(501))
                .andExpect(jsonPath("$.data.posts[0].gallery.galleryId").value(10))
                .andExpect(jsonPath("$.data.posts[0].gallery.name").value("음악"))
                .andExpect(jsonPath("$.data.posts[0].writer.nickname").value("writer"))
                .andExpect(jsonPath("$.data.posts[0].title").value("제목"))
                .andExpect(jsonPath("$.data.posts[0].contentPreview").value("내용"))
                .andExpect(jsonPath("$.data.posts[0].likeCount").value(9))
                .andExpect(jsonPath("$.data.posts[0].commentCount").value(2));
    }

    @Test
    void 페이지_파라미터를_유스케이스에_넘긴다() throws Exception {
        when(getPopularPostsUseCase.execute(2, 5)).thenReturn(new PopularPostListResult(2, 5, 0L, List.of()));

        mockMvc.perform(get("/api/v1/posts/popular").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2));
    }

    @Test
    void 잘못된_페이지면_400을_반환한다() throws Exception {
        when(getPopularPostsUseCase.execute(-1, 20)).thenThrow(
                new PostApplicationException(PostErrorCode.VALIDATION_ERROR, "페이지 번호와 크기를 확인해 주세요."));

        mockMvc.perform(get("/api/v1/posts/popular").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
