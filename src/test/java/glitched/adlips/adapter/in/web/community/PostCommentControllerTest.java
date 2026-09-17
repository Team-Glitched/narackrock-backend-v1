package glitched.adlips.adapter.in.web.community;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.community.CreatePostCommentUseCase;
import glitched.adlips.application.community.PostCommentApplicationException;
import glitched.adlips.application.community.PostCommentErrorCode;
import glitched.adlips.application.community.PostCommentResult;
import glitched.adlips.application.community.DeletePostCommentUseCase;
import glitched.adlips.application.community.UpdatePostCommentUseCase;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PostCommentController.class)
@Import(SecurityConfig.class)
class PostCommentControllerTest {

    @Test
    void 댓글_조회는_인증이_필요하다() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/posts/10/comments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 댓글_조회_페이지를_반환한다() throws Exception {
        when(getPostCommentsUseCase.execute(10L, null, 0, 20)).thenReturn(
                new glitched.adlips.application.community.PostCommentListResult(10L, null, 0, 20, 0, java.util.List.of()));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/posts/10/comments")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.comments").isArray());
    }

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean CreatePostCommentUseCase createPostCommentUseCase;
    @MockitoBean glitched.adlips.application.community.GetPostCommentsUseCase getPostCommentsUseCase;
    @MockitoBean UpdatePostCommentUseCase updatePostCommentUseCase;
    @MockitoBean DeletePostCommentUseCase deletePostCommentUseCase;
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
    void 댓글_작성시_201과_commentId를_반환한다() throws Exception {
        when(createPostCommentUseCase.execute(10L, 1L, "좋은 코드 진행이네요.", null))
                .thenReturn(new PostCommentResult(10L, 7721L, null));

        mockMvc.perform(post("/api/v1/posts/10/comments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"좋은 코드 진행이네요.\",\"parentCommentId\":null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글이 성공적으로 등록되었습니다."))
                .andExpect(jsonPath("$.data.commentId").value(7721));
    }

    @Test
    void 대댓글_작성시_201과_대댓글_안내_문구를_반환한다() throws Exception {
        when(createPostCommentUseCase.execute(10L, 1L, "동의합니다", 7721L))
                .thenReturn(new PostCommentResult(10L, 7722L, 7721L));

        mockMvc.perform(post("/api/v1/posts/10/comments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"동의합니다\",\"parentCommentId\":7721}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("대댓글이 성공적으로 등록되었습니다."))
                .andExpect(jsonPath("$.data.commentId").value(7722));
    }

    @Test
    void 존재하지_않는_게시글이면_404와_POST_NOT_FOUND를_반환한다() throws Exception {
        when(createPostCommentUseCase.execute(999L, 1L, "좋은 곡이네요", null)).thenThrow(
                new PostCommentApplicationException(
                        PostCommentErrorCode.POST_NOT_FOUND,
                        "존재하지 않거나 삭제된 게시글에는 댓글을 작성할 수 없습니다."));

        mockMvc.perform(post("/api/v1/posts/999/comments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"좋은 곡이네요\",\"parentCommentId\":null}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않거나 삭제된 게시글에는 댓글을 작성할 수 없습니다."));
    }

    @Test
    void 정지된_사용자면_403과_BANNED_USER_ACCESS를_반환한다() throws Exception {
        when(createPostCommentUseCase.execute(10L, 1L, "좋은 곡이네요", null)).thenThrow(
                new PostCommentApplicationException(
                        PostCommentErrorCode.BANNED_USER_ACCESS,
                        "현재 서비스 이용 정지 상태이므로 댓글을 작성할 수 없습니다."));

        mockMvc.perform(post("/api/v1/posts/10/comments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"좋은 곡이네요\",\"parentCommentId\":null}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("BANNED_USER_ACCESS"));
    }

    @Test
    void 내용이_공백이면_400과_INVALID_INPUT_VALUE를_반환한다() throws Exception {
        when(createPostCommentUseCase.execute(10L, 1L, "  ", null)).thenThrow(
                new PostCommentApplicationException(
                        PostCommentErrorCode.INVALID_INPUT_VALUE, "댓글 내용은 필수 입력 사항입니다."));

        mockMvc.perform(post("/api/v1/posts/10/comments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  \",\"parentCommentId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 부모_댓글이_없으면_404와_PARENT_COMMENT_NOT_FOUND를_반환한다() throws Exception {
        when(createPostCommentUseCase.execute(10L, 1L, "동의합니다", 404L)).thenThrow(
                new PostCommentApplicationException(
                        PostCommentErrorCode.PARENT_COMMENT_NOT_FOUND,
                        "존재하지 않거나 삭제된 댓글에는 대댓글을 작성할 수 없습니다."));

        mockMvc.perform(post("/api/v1/posts/10/comments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"동의합니다\",\"parentCommentId\":404}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PARENT_COMMENT_NOT_FOUND"));
    }

    @Test
    void 인증_헤더가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"좋은 곡이네요\",\"parentCommentId\":null}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 댓글_수정시_200과_commentId를_반환한다() throws Exception {
        when(updatePostCommentUseCase.execute(7721L, 1L, "수정된 댓글 내용입니다.")).thenReturn(7721L);

        mockMvc.perform(put("/api/v1/posts/comments/7721")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글 내용입니다.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글이 성공적으로 수정되었습니다."))
                .andExpect(jsonPath("$.data.commentId").value(7721));
    }

    @Test
    void 댓글_수정시_작성자가_아니면_403과_NOT_COMMENT_OWNER를_반환한다() throws Exception {
        when(updatePostCommentUseCase.execute(7721L, 1L, "수정된 댓글 내용입니다.")).thenThrow(
                new PostCommentApplicationException(
                        PostCommentErrorCode.NOT_COMMENT_OWNER, "본인이 작성한 댓글만 수정할 수 있습니다."));

        mockMvc.perform(put("/api/v1/posts/comments/7721")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글 내용입니다.\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_COMMENT_OWNER"))
                .andExpect(jsonPath("$.message").value("본인이 작성한 댓글만 수정할 수 있습니다."));
    }

    @Test
    void 댓글_수정시_정지된_사용자면_403과_BANNED_USER_ACCESS를_반환한다() throws Exception {
        when(updatePostCommentUseCase.execute(7721L, 1L, "수정된 댓글 내용입니다.")).thenThrow(
                new PostCommentApplicationException(
                        PostCommentErrorCode.BANNED_USER_ACCESS,
                        "현재 서비스 이용 정지 상태이므로 댓글을 수정할 수 없습니다."));

        mockMvc.perform(put("/api/v1/posts/comments/7721")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글 내용입니다.\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("BANNED_USER_ACCESS"));
    }

    @Test
    void 댓글_수정시_댓글이_없으면_404와_COMMENT_NOT_FOUND를_반환한다() throws Exception {
        when(updatePostCommentUseCase.execute(404L, 1L, "수정된 댓글 내용입니다.")).thenThrow(
                new PostCommentApplicationException(
                        PostCommentErrorCode.COMMENT_NOT_FOUND,
                        "수정하려는 댓글을 찾을 수 없거나 이미 삭제되었습니다."));

        mockMvc.perform(put("/api/v1/posts/comments/404")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글 내용입니다.\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("수정하려는 댓글을 찾을 수 없거나 이미 삭제되었습니다."));
    }

    @Test
    void 댓글_수정시_내용이_공백이면_400과_INVALID_INPUT_VALUE를_반환한다() throws Exception {
        when(updatePostCommentUseCase.execute(7721L, 1L, "  ")).thenThrow(
                new PostCommentApplicationException(
                        PostCommentErrorCode.INVALID_INPUT_VALUE, "댓글 내용은 필수 입력 사항입니다."));

        mockMvc.perform(put("/api/v1/posts/comments/7721")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 댓글_수정은_인증이_필요하다() throws Exception {
        mockMvc.perform(put("/api/v1/posts/comments/7721")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글 내용입니다.\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 댓글_삭제시_200을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/comments/7721")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글이 성공적으로 삭제되었습니다."));

        verify(deletePostCommentUseCase).execute(7721L, 1L);
    }

    @Test
    void 댓글_삭제시_작성자가_아니면_403과_NOT_COMMENT_OWNER를_반환한다() throws Exception {
        doThrow(new PostCommentApplicationException(
                PostCommentErrorCode.NOT_COMMENT_OWNER, "본인이 작성한 댓글만 삭제할 수 있습니다."))
                .when(deletePostCommentUseCase).execute(7721L, 1L);

        mockMvc.perform(delete("/api/v1/posts/comments/7721")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_COMMENT_OWNER"))
                .andExpect(jsonPath("$.message").value("본인이 작성한 댓글만 삭제할 수 있습니다."));
    }

    @Test
    void 댓글_삭제시_정지된_사용자면_403과_BANNED_USER_ACCESS를_반환한다() throws Exception {
        doThrow(new PostCommentApplicationException(
                PostCommentErrorCode.BANNED_USER_ACCESS,
                "현재 서비스 이용 정지 상태이므로 댓글을 삭제할 수 없습니다."))
                .when(deletePostCommentUseCase).execute(7721L, 1L);

        mockMvc.perform(delete("/api/v1/posts/comments/7721")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("BANNED_USER_ACCESS"));
    }

    @Test
    void 댓글_삭제시_댓글이_없으면_404와_COMMENT_NOT_FOUND를_반환한다() throws Exception {
        doThrow(new PostCommentApplicationException(
                PostCommentErrorCode.COMMENT_NOT_FOUND, "존재하지 않거나 이미 삭제된 댓글입니다."))
                .when(deletePostCommentUseCase).execute(404L, 1L);

        mockMvc.perform(delete("/api/v1/posts/comments/404")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않거나 이미 삭제된 댓글입니다."));
    }

    @Test
    void 댓글_삭제는_인증이_필요하다() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/comments/7721"))
                .andExpect(status().isUnauthorized());
    }
}
