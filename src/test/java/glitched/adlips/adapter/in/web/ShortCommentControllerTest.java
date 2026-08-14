package glitched.adlips.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.shorts.ShortCommentController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.shorts.DeleteShortCommentUseCase;
import glitched.adlips.application.shorts.GetShortCommentsUseCase;
import glitched.adlips.application.shorts.ShortCommentApplicationException;
import glitched.adlips.application.shorts.ShortCommentErrorCode;
import glitched.adlips.application.shorts.ShortCommentLikeResult;
import glitched.adlips.application.shorts.ShortCommentListResult;
import glitched.adlips.application.shorts.ShortCommentReportResult;
import glitched.adlips.application.shorts.ShortCommentResult;
import glitched.adlips.application.shorts.SubmitShortCommentReportUseCase;
import glitched.adlips.application.shorts.SubmitShortCommentUseCase;
import glitched.adlips.application.shorts.ToggleShortCommentLikeUseCase;
import glitched.adlips.application.shorts.UpdateShortCommentUseCase;
import glitched.adlips.application.shorts.port.out.ShortCommentQueryItem;
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

@WebMvcTest(ShortCommentController.class)
@Import(SecurityConfig.class)
class ShortCommentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean SubmitShortCommentUseCase submitShortCommentUseCase;
    @MockitoBean GetShortCommentsUseCase getShortCommentsUseCase;
    @MockitoBean UpdateShortCommentUseCase updateShortCommentUseCase;
    @MockitoBean DeleteShortCommentUseCase deleteShortCommentUseCase;
    @MockitoBean ToggleShortCommentLikeUseCase toggleShortCommentLikeUseCase;
    @MockitoBean SubmitShortCommentReportUseCase submitShortCommentReportUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    String validToken;

    @BeforeEach
    void setUp() {
        validToken = jwtTokenIssuerAdapter.issue(1L);
        when(accessTokenPort.verify(validToken)).thenReturn(1L);
        when(authenticatedUserResolver.requireUserId("Bearer " + validToken)).thenReturn(1L);
        when(authenticatedUserResolver.resolveOptionalUserId("Bearer " + validToken)).thenReturn(1L);
        when(authenticatedUserResolver.resolveOptionalUserId(null)).thenReturn(null);
    }

    @Test
    void 로그인_상태로_댓글_목록_조회시_200과_목록을_반환한다() throws Exception {
        ShortCommentQueryItem item = new ShortCommentQueryItem(
                7721L, null, "멜로디가 좋아요.", 12L, "adlip_user", "https://cdn.test/img.png",
                4, 2, false, LocalDateTime.of(2026, 6, 22, 14, 0));
        when(getShortCommentsUseCase.execute(15L, 1L))
                .thenReturn(new ShortCommentListResult(15L, List.of(item)));

        mockMvc.perform(get("/api/v1/shorts/15/comments")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.shortId").value(15))
                .andExpect(jsonPath("$.data.comments[0].commentId").value(7721))
                .andExpect(jsonPath("$.data.comments[0].parentCommentId").doesNotExist())
                .andExpect(jsonPath("$.data.comments[0].content").value("멜로디가 좋아요."))
                .andExpect(jsonPath("$.data.comments[0].writer.userId").value(12))
                .andExpect(jsonPath("$.data.comments[0].writer.nickname").value("adlip_user"))
                .andExpect(jsonPath("$.data.comments[0].writer.profileImageUrl").value("https://cdn.test/img.png"))
                .andExpect(jsonPath("$.data.comments[0].likeCount").value(4))
                .andExpect(jsonPath("$.data.comments[0].replyCount").value(2))
                .andExpect(jsonPath("$.data.comments[0].isLiked").value(false))
                .andExpect(jsonPath("$.data.comments[0].createdAt").value("2026-06-22T14:00:00"));
    }

    @Test
    void 비로그인_상태로_댓글_목록을_조회해도_200을_반환한다() throws Exception {
        when(getShortCommentsUseCase.execute(15L, null))
                .thenReturn(new ShortCommentListResult(15L, List.of()));

        mockMvc.perform(get("/api/v1/shorts/15/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortId").value(15));
    }

    @Test
    void 댓글_목록_조회시_존재하지_않는_숏폼이면_404를_반환한다() throws Exception {
        when(getShortCommentsUseCase.execute(999L, null)).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.SHORT_NOT_FOUND, "존재하지 않는 숏폼입니다."));

        mockMvc.perform(get("/api/v1/shorts/999/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SHORT_NOT_FOUND"));
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

    @Test
    void 댓글_수정시_200과_commentId를_반환한다() throws Exception {
        when(updateShortCommentUseCase.execute(15L, 7721L, 1L, "수정된 댓글 내용입니다."))
                .thenReturn(7721L);

        mockMvc.perform(put("/api/v1/shorts/15/comments/7721")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글 내용입니다.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글이 성공적으로 수정되었습니다."))
                .andExpect(jsonPath("$.data.commentId").value(7721));
    }

    @Test
    void 댓글_수정시_내용이_공백이면_400을_반환한다() throws Exception {
        when(updateShortCommentUseCase.execute(15L, 7721L, 1L, "  ")).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.INVALID_INPUT_VALUE, "댓글 내용은 필수 입력 사항입니다."));

        mockMvc.perform(put("/api/v1/shorts/15/comments/7721")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 댓글_수정시_본인_댓글이_아니면_403을_반환한다() throws Exception {
        when(updateShortCommentUseCase.execute(15L, 7721L, 1L, "내용")).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.NOT_COMMENT_OWNER, "본인이 작성한 댓글만 수정할 수 있습니다."));

        mockMvc.perform(put("/api/v1/shorts/15/comments/7721")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"내용\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_COMMENT_OWNER"));
    }

    @Test
    void 댓글_수정시_존재하지_않으면_404를_반환한다() throws Exception {
        when(updateShortCommentUseCase.execute(15L, 999L, 1L, "내용")).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.COMMENT_NOT_FOUND, "수정하려는 댓글을 찾을 수 없거나 이미 삭제되었습니다."));

        mockMvc.perform(put("/api/v1/shorts/15/comments/999")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"내용\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void 댓글_수정시_인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(put("/api/v1/shorts/15/comments/7721")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"내용\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 댓글_삭제시_200과_data_없이_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/shorts/15/comments/7721")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글이 성공적으로 삭제되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 댓글_삭제시_본인_댓글이_아니면_403을_반환한다() throws Exception {
        doThrow(new ShortCommentApplicationException(
                ShortCommentErrorCode.NOT_COMMENT_OWNER, "본인이 작성한 댓글만 삭제할 수 있습니다."))
                .when(deleteShortCommentUseCase).execute(15L, 7721L, 1L);

        mockMvc.perform(delete("/api/v1/shorts/15/comments/7721")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_COMMENT_OWNER"));
    }

    @Test
    void 댓글_삭제시_존재하지_않으면_404를_반환한다() throws Exception {
        doThrow(new ShortCommentApplicationException(
                ShortCommentErrorCode.COMMENT_NOT_FOUND, "존재하지 않거나 이미 삭제된 댓글입니다."))
                .when(deleteShortCommentUseCase).execute(15L, 999L, 1L);

        mockMvc.perform(delete("/api/v1/shorts/15/comments/999")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void 댓글_삭제시_인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/shorts/15/comments/7721"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 댓글_좋아요_토글시_200과_결과를_반환한다() throws Exception {
        when(toggleShortCommentLikeUseCase.toggle(1L, 15L, 7721L))
                .thenReturn(new ShortCommentLikeResult(7721L, true, 5));

        mockMvc.perform(post("/api/v1/shorts/15/comments/7721/likes")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글 좋아요 상태가 성공적으로 반영되었습니다."))
                .andExpect(jsonPath("$.data.commentId").value(7721))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(5));
    }

    @Test
    void 댓글_좋아요_취소시_isLiked_false를_반환한다() throws Exception {
        when(toggleShortCommentLikeUseCase.toggle(1L, 15L, 7721L))
                .thenReturn(new ShortCommentLikeResult(7721L, false, 4));

        mockMvc.perform(post("/api/v1/shorts/15/comments/7721/likes")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(4));
    }

    @Test
    void 댓글_좋아요_토글시_존재하지_않으면_404를_반환한다() throws Exception {
        when(toggleShortCommentLikeUseCase.toggle(1L, 15L, 999L)).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.COMMENT_NOT_FOUND,
                        "존재하지 않거나 이미 삭제된 댓글에는 좋아요를 누를 수 없습니다."));

        mockMvc.perform(post("/api/v1/shorts/15/comments/999/likes")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void 댓글_좋아요_토글시_인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/shorts/15/comments/7721/likes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 댓글_신고시_201과_신고_결과를_반환한다() throws Exception {
        when(submitShortCommentReportUseCase.execute(
                15L, 7721L, 1L, "욕설/비방", "특정 사용자를 비방하는 내용이 포함되어 있습니다."))
                .thenReturn(new ShortCommentReportResult(303L, 7721L));

        mockMvc.perform(post("/api/v1/shorts/15/comments/7721/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"욕설/비방\",\"description\":\"특정 사용자를 비방하는 내용이 포함되어 있습니다.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글 신고가 성공적으로 접수되었습니다."))
                .andExpect(jsonPath("$.data.reportId").value(303))
                .andExpect(jsonPath("$.data.targetType").value("SHORT_COMMENT"))
                .andExpect(jsonPath("$.data.targetId").value(7721));
    }

    @Test
    void 댓글_신고시_사유가_공백이면_400을_반환한다() throws Exception {
        when(submitShortCommentReportUseCase.execute(15L, 7721L, 1L, "  ", "설명")).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.INVALID_INPUT_VALUE, "신고 사유를 입력해 주세요."));

        mockMvc.perform(post("/api/v1/shorts/15/comments/7721/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  \",\"description\":\"설명\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 댓글_신고시_본인_댓글이면_400을_반환한다() throws Exception {
        when(submitShortCommentReportUseCase.execute(15L, 7721L, 1L, "욕설/비방", "설명")).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.CANNOT_REPORT_OWN_COMMENT, "본인이 작성한 댓글은 신고할 수 없습니다."));

        mockMvc.perform(post("/api/v1/shorts/15/comments/7721/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"욕설/비방\",\"description\":\"설명\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CANNOT_REPORT_OWN_COMMENT"));
    }

    @Test
    void 댓글_신고시_존재하지_않으면_404를_반환한다() throws Exception {
        when(submitShortCommentReportUseCase.execute(15L, 999L, 1L, "욕설/비방", "설명")).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.COMMENT_NOT_FOUND, "존재하지 않거나 이미 삭제된 댓글은 신고할 수 없습니다."));

        mockMvc.perform(post("/api/v1/shorts/15/comments/999/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"욕설/비방\",\"description\":\"설명\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void 댓글_신고시_이미_신고했으면_409를_반환한다() throws Exception {
        when(submitShortCommentReportUseCase.execute(15L, 7721L, 1L, "욕설/비방", "설명")).thenThrow(
                new ShortCommentApplicationException(
                        ShortCommentErrorCode.ALREADY_REPORTED, "이미 신고한 댓글입니다."));

        mockMvc.perform(post("/api/v1/shorts/15/comments/7721/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"욕설/비방\",\"description\":\"설명\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_REPORTED"));
    }

    @Test
    void 댓글_신고시_인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/shorts/15/comments/7721/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"욕설/비방\",\"description\":\"설명\"}"))
                .andExpect(status().isUnauthorized());
    }
}
