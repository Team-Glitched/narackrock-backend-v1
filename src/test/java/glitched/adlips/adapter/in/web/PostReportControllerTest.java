package glitched.adlips.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.community.PostReportController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.report.usecase.ReportSubmissionApplicationException;
import glitched.adlips.application.report.usecase.ReportSubmissionErrorCode;
import glitched.adlips.application.report.usecase.ReportSubmissionResult;
import glitched.adlips.application.report.usecase.SubmitReportUseCase;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.domain.report.ReportTargetType;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PostReportController.class)
@Import(SecurityConfig.class)
class PostReportControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean SubmitReportUseCase submitReportUseCase;
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
    void 게시글_신고는_201과_명세_응답을_반환한다() throws Exception {
        when(submitReportUseCase.execute(
                ReportTargetType.POST, 452L, 1L, "욕설/비방", "비방 내용"
        )).thenReturn(new ReportSubmissionResult(301L, ReportTargetType.POST, 452L));

        mockMvc.perform(post("/api/v1/posts/452/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"욕설/비방\",\"description\":\"비방 내용\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 신고가 성공적으로 접수되었습니다."))
                .andExpect(jsonPath("$.data.reportId").value(301))
                .andExpect(jsonPath("$.data.targetType").value("POST"))
                .andExpect(jsonPath("$.data.targetId").value(452));
    }

    @Test
    void 게시글_댓글_신고는_201과_명세_응답을_반환한다() throws Exception {
        when(submitReportUseCase.execute(
                ReportTargetType.POST_COMMENT, 7721L, 1L, "욕설/비방", "비방 내용"
        )).thenReturn(new ReportSubmissionResult(
                302L, ReportTargetType.POST_COMMENT, 7721L));

        mockMvc.perform(post("/api/v1/posts/comments/7721/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"욕설/비방\",\"description\":\"비방 내용\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("댓글 신고가 성공적으로 접수되었습니다."))
                .andExpect(jsonPath("$.data.reportId").value(302))
                .andExpect(jsonPath("$.data.targetType").value("POST_COMMENT"))
                .andExpect(jsonPath("$.data.targetId").value(7721));
    }

    @Test
    void 본인_게시글_신고는_400을_반환한다() throws Exception {
        when(submitReportUseCase.execute(
                ReportTargetType.POST, 452L, 1L, "욕설/비방", null
        )).thenThrow(new ReportSubmissionApplicationException(
                ReportSubmissionErrorCode.CANNOT_REPORT_OWN_POST,
                "본인이 작성한 게시글은 신고할 수 없습니다."
        ));

        mockMvc.perform(post("/api/v1/posts/452/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"욕설/비방\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CANNOT_REPORT_OWN_POST"));
    }

    @Test
    void 중복_댓글_신고는_409를_반환한다() throws Exception {
        when(submitReportUseCase.execute(
                ReportTargetType.POST_COMMENT, 7721L, 1L, "욕설/비방", null
        )).thenThrow(new ReportSubmissionApplicationException(
                ReportSubmissionErrorCode.ALREADY_REPORTED,
                "이미 신고한 댓글입니다."
        ));

        mockMvc.perform(post("/api/v1/posts/comments/7721/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"욕설/비방\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_REPORTED"));
    }

    @Test
    void 인증없는_신고는_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/posts/452/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"욕설/비방\"}"))
                .andExpect(status().isUnauthorized());
    }
}
