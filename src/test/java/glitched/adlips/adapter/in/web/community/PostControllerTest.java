package glitched.adlips.adapter.in.web.community;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.community.CreatePostUseCase;
import glitched.adlips.application.community.DeletePostUseCase;
import glitched.adlips.application.community.GetPostUseCase;
import glitched.adlips.application.community.GetPostsUseCase;
import glitched.adlips.application.community.PostApplicationException;
import glitched.adlips.application.community.PostErrorCode;
import glitched.adlips.application.community.PostListResult;
import glitched.adlips.application.community.PostResult;
import glitched.adlips.application.community.UpdatePostUseCase;
import glitched.adlips.application.community.port.out.PostQueryItem;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.global.config.SecurityConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PostController.class)
@Import(SecurityConfig.class)
class PostControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean CreatePostUseCase createPostUseCase;
    @MockitoBean GetPostUseCase getPostUseCase;
    @MockitoBean GetPostsUseCase getPostsUseCase;
    @MockitoBean UpdatePostUseCase updatePostUseCase;
    @MockitoBean DeletePostUseCase deletePostUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    String validToken;

    @BeforeEach
    void setUp() {
        validToken = jwtTokenIssuerAdapter.issue(1L);
        when(accessTokenPort.verify(validToken)).thenReturn(1L);
        when(authenticatedUserResolver.requireUserId("Bearer " + validToken)).thenReturn(1L);
    }

    @Test
    void 게시글_생성시_201과_결과를_반환한다() throws Exception {
        when(createPostUseCase.execute(10L, 1L, "제목", "내용"))
                .thenReturn(new PostResult(501L, 10L));

        mockMvc.perform(post("/api/v1/galleries/10/posts")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.postId").value(501));
    }

    @Test
    void 게시글_생성시_제목이_공백이면_400을_반환한다() throws Exception {
        when(createPostUseCase.execute(10L, 1L, "  ", "내용")).thenThrow(
                new PostApplicationException(PostErrorCode.VALIDATION_ERROR, "제목은 필수 입력 사항입니다."));

        mockMvc.perform(post("/api/v1/galleries/10/posts")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \",\"content\":\"내용\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void 게시글_생성시_존재하지_않는_갤러리면_404를_반환한다() throws Exception {
        when(createPostUseCase.execute(999L, 1L, "제목", "내용")).thenThrow(
                new PostApplicationException(PostErrorCode.GALLERY_NOT_FOUND, "존재하지 않는 갤러리에는 게시글을 작성할 수 없습니다."));

        mockMvc.perform(post("/api/v1/galleries/999/posts")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("GALLERY_NOT_FOUND"));
    }

    @Test
    void 게시글_생성시_인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/galleries/10/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 게시글_목록_조회시_인증없이도_200과_목록을_반환한다() throws Exception {
        PostQueryItem item = new PostQueryItem(
                501L, 10L, 1L, "writer", "https://cdn.test/img.png", "제목", "내용",
                3, 2, 0, 1, LocalDateTime.of(2026, 8, 23, 10, 0));
        when(getPostsUseCase.execute(10L, 0, 20, null, "LATEST"))
                .thenReturn(new PostListResult(10L, 1L, List.of(item)));

        mockMvc.perform(get("/api/v1/galleries/10/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.galleryId").value(10))
                .andExpect(jsonPath("$.data.currentSort").value("LATEST"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.posts[0].postId").value(501))
                .andExpect(jsonPath("$.data.posts[0].writer.nickname").value("writer"))
                .andExpect(jsonPath("$.data.posts[0].title").value("제목"));
    }

    @Test
    void 게시글_목록_조회시_존재하지_않는_갤러리면_404를_반환한다() throws Exception {
        when(getPostsUseCase.execute(999L, 0, 20, null, "LATEST")).thenThrow(
                new PostApplicationException(PostErrorCode.GALLERY_NOT_FOUND, "존재하지 않는 갤러리입니다."));

        mockMvc.perform(get("/api/v1/galleries/999/posts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("GALLERY_NOT_FOUND"));
    }

    @Test
    void 게시글_상세_조회시_인증된_사용자에게_200과_상세정보를_반환한다() throws Exception {
        PostQueryItem item = new PostQueryItem(
                501L, 10L, 1L, "writer", null, "제목", "내용",
                3, 2, 0, 1, LocalDateTime.of(2026, 8, 23, 10, 0));
        when(getPostUseCase.execute(501L, 1L)).thenReturn(item);

        mockMvc.perform(get("/api/v1/posts/501")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(501))
                .andExpect(jsonPath("$.data.content").value("내용"))
                .andExpect(jsonPath("$.data.writer.userId").value(1));
    }

    @Test
    void 게시글_상세_조회시_인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/posts/501"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 게시글_목록_조회시_검색어와_정렬조건을_전달한다() throws Exception {
        when(getPostsUseCase.execute(10L, 0, 20, "기타", "POPULAR"))
                .thenReturn(new PostListResult(10L, "자유", glitched.adlips.application.community.PostSort.POPULAR,
                        "기타", 0L, List.of()));

        mockMvc.perform(get("/api/v1/galleries/10/posts")
                        .param("keyword", "기타")
                        .param("sort", "POPULAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.galleryName").value("자유"))
                .andExpect(jsonPath("$.data.currentSort").value("POPULAR"))
                .andExpect(jsonPath("$.data.searchedKeyword").value("기타"));
    }

    @Test
    void 게시글_상세_조회시_존재하지_않으면_404를_반환한다() throws Exception {
        when(getPostUseCase.execute(999L, 1L)).thenThrow(
                new PostApplicationException(PostErrorCode.POST_NOT_FOUND, "존재하지 않거나 이미 삭제된 게시글입니다."));

        mockMvc.perform(get("/api/v1/posts/999")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("POST_NOT_FOUND"));
    }

    @Test
    void 게시글_수정시_200과_postId를_반환한다() throws Exception {
        when(updatePostUseCase.execute(501L, 1L, "수정된 제목", "수정된 내용")).thenReturn(501L);

        mockMvc.perform(put("/api/v1/posts/501")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"수정된 제목\",\"content\":\"수정된 내용\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(501));
    }

    @Test
    void 게시글_수정시_본인_게시글이_아니면_403을_반환한다() throws Exception {
        when(updatePostUseCase.execute(501L, 1L, "제목", "내용")).thenThrow(
                new PostApplicationException(PostErrorCode.NOT_POST_OWNER, "본인이 작성한 게시글만 수정할 수 있습니다."));

        mockMvc.perform(put("/api/v1/posts/501")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_POST_OWNER"));
    }

    @Test
    void 게시글_수정시_인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(put("/api/v1/posts/501")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 게시글_삭제시_200과_data_없이_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/501")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 게시글_삭제시_본인_게시글이_아니면_403을_반환한다() throws Exception {
        doThrow(new PostApplicationException(PostErrorCode.NOT_POST_OWNER, "본인이 작성한 게시글만 삭제할 수 있습니다."))
                .when(deletePostUseCase).execute(501L, 1L);

        mockMvc.perform(delete("/api/v1/posts/501")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_POST_OWNER"));
    }

    @Test
    void 게시글_삭제시_인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/501"))
                .andExpect(status().isUnauthorized());
    }
}
