package glitched.adlips.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.shorts.ShortCommentController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.shorts.ShortCommentApplicationException;
import glitched.adlips.application.shorts.ShortCommentErrorCode;
import glitched.adlips.application.shorts.ShortCommentResult;
import glitched.adlips.application.shorts.SubmitShortCommentUseCase;
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

@WebMvcTest(ShortCommentController.class)
@Import(SecurityConfig.class)
class ShortCommentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean SubmitShortCommentUseCase submitShortCommentUseCase;
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
    void 댓글_생성시_201과_댓글_메시지를_반환한다() throws Exception {
        when(submitShortCommentUseCase.execute(15L, 1L, "멜로디가 좋아요.", null))
                .thenReturn(new ShortCommentResult(15L, 7721L, null));

        mockMvc.perform(post("/api/v1/shorts/15/comments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"멜로디가 좋아요.\",\"parentCommentId\":null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글이 성공적으로 등록되었습니다."))
                .andExpect(jsonPath("$.data.shortId").value(15))
                .andExpect(jsonPath("$.data.commentId").value(7721))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(content().string(containsString("\"parentCommentId\":null")));
    }

    @Test
    void 대댓글_생성시_201과_대댓글_메시지를_반환한다() throws Exception {
        when(submitShortCommentUseCase.execute(15L, 1L, "저도 이 부분 좋다고 생각해요.", 7721L))
                .thenReturn(new ShortCommentResult(15L, 7730L, 7721L));

        mockMvc.perform(post("/api/v1/shorts/15/comments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"저도 이 부분 좋다고 생각해요.\",\"parentCommentId\":7721}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("대댓글이 성공적으로 등록되었습니다."))
                .andExpect(jsonPath("$.data.commentId").value(7730))
                .andExpect(jsonPath("$.data.parentCommentId").value(7721));
    }

    @Test
    void 내용이_공백이면_400을_반환한다() throws Exception {
        when(submitShortCommentUseCase.execute(15L, 1L, "  ", null)).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.INVALID_INPUT_VALUE, "댓글 내용은 필수 입력 사항입니다."));

        mockMvc.perform(post("/api/v1/shorts/15/comments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  \",\"parentCommentId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.message").value("댓글 내용은 필수 입력 사항입니다."));
    }

    @Test
    void 존재하지_않는_숏폼이면_404를_반환한다() throws Exception {
        when(submitShortCommentUseCase.execute(999L, 1L, "내용", null)).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.SHORT_NOT_FOUND, "존재하지 않거나 삭제된 숏폼에는 댓글을 작성할 수 없습니다."));

        mockMvc.perform(post("/api/v1/shorts/999/comments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"내용\",\"parentCommentId\":null}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SHORT_NOT_FOUND"));
    }

    @Test
    void 존재하지_않는_부모_댓글이면_404를_반환한다() throws Exception {
        when(submitShortCommentUseCase.execute(15L, 1L, "내용", 999L)).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.PARENT_COMMENT_NOT_FOUND, "존재하지 않거나 삭제된 댓글에는 대댓글을 작성할 수 없습니다."));

        mockMvc.perform(post("/api/v1/shorts/15/comments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"내용\",\"parentCommentId\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PARENT_COMMENT_NOT_FOUND"));
    }

    @Test
    void 이용_정지_사용자면_403을_반환한다() throws Exception {
        when(submitShortCommentUseCase.execute(15L, 1L, "내용", null)).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.BANNED_USER_ACCESS,
                        "현재 서비스 이용 정지 상태이므로 댓글을 작성할 수 없습니다."));

        mockMvc.perform(post("/api/v1/shorts/15/comments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"내용\",\"parentCommentId\":null}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("BANNED_USER_ACCESS"))
                .andExpect(jsonPath("$.message")
                        .value("현재 서비스 이용 정지 상태이므로 댓글을 작성할 수 없습니다."));
    }

    @Test
    void 인증없는_요청시_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/shorts/15/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"내용\",\"parentCommentId\":null}"))
                .andExpect(status().isUnauthorized());
    }
}
