package glitched.adlips.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.admin.AdminReportController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.report.usecase.ReportResolutionApplicationException;
import glitched.adlips.application.report.usecase.ReportResolutionErrorCode;
import glitched.adlips.application.report.usecase.ReportResolutionResult;
import glitched.adlips.application.report.usecase.RejectReportUseCase;
import glitched.adlips.application.report.usecase.ResolveReportUseCase;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.domain.report.ModerationActionType;
import glitched.adlips.domain.report.ReportStatus;
import glitched.adlips.domain.report.ReportTargetType;
import glitched.adlips.global.config.SecurityConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminReportController.class)
@Import(SecurityConfig.class)
class AdminReportControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean ResolveReportUseCase resolveReportUseCase;
    @MockitoBean RejectReportUseCase rejectReportUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    String validToken;

    @BeforeEach
    void setUp() {
        validToken = jwtTokenIssuerAdapter.issue(2L);
        when(accessTokenPort.verify(validToken)).thenReturn(2L);
        when(authenticatedUserResolver.requireUserId("Bearer " + validToken)).thenReturn(2L);
    }

    @Test
    void 유효한_요청시_200과_처리_결과를_반환한다() throws Exception {
        when(resolveReportUseCase.execute(101L, 2L, "HIDE_CONTENT", "운영 정책 위반으로 숨김 처리합니다."))
                .thenReturn(new ReportResolutionResult(
                        101L, ReportStatus.RESOLVED, ModerationActionType.HIDE_CONTENT,
                        LocalDateTime.of(2026, 6, 24, 14, 40), ReportTargetType.SHORT, 12L));

        mockMvc.perform(patch("/api/v1/admin/reports/101/resolve")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"HIDE_CONTENT\",\"reason\":\"운영 정책 위반으로 숨김 처리합니다.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("해당 신고 내역이 성공적으로 처리되었습니다."))
                .andExpect(jsonPath("$.data.reportId").value(101))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.actionType").value("HIDE_CONTENT"))
                .andExpect(jsonPath("$.data.handledAt").value("2026-06-24T14:40:00"))
                .andExpect(jsonPath("$.data.targetType").value("SHORT"))
                .andExpect(jsonPath("$.data.targetId").value(12));
    }

    @Test
    void 반려_요청시_명세의_PATCH_경로로_200과_반려_결과를_반환한다() throws Exception {
        when(rejectReportUseCase.execute(101L, 2L, "정책 위반으로 보기 어렵습니다."))
                .thenReturn(new ReportResolutionResult(
                        101L, ReportStatus.REJECTED, ModerationActionType.REJECT_REPORT,
                        LocalDateTime.of(2026, 6, 24, 14, 45), ReportTargetType.SHORT, 12L));

        mockMvc.perform(patch("/api/v1/admin/reports/101/reject")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"정책 위반으로 보기 어렵습니다.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("신고가 반려 처리되었습니다."))
                .andExpect(jsonPath("$.data.reportId").value(101))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.handledAt").value("2026-06-24T14:45:00"))
                .andExpect(jsonPath("$.data.actionType").doesNotExist());
    }

    @Test
    void reason이_공백이면_400을_반환한다() throws Exception {
        when(resolveReportUseCase.execute(101L, 2L, "HIDE_CONTENT", "  ")).thenThrow(
                new ReportResolutionApplicationException(
                        ReportResolutionErrorCode.VALIDATION_ERROR, "차단 원인을 입력해 주세요."));

        mockMvc.perform(patch("/api/v1/admin/reports/101/resolve")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"HIDE_CONTENT\",\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void 관리자가_아니면_403을_반환한다() throws Exception {
        when(resolveReportUseCase.execute(101L, 2L, "HIDE_CONTENT", "사유")).thenThrow(
                new ReportResolutionApplicationException(
                        ReportResolutionErrorCode.FORBIDDEN, "관리자만 처리할 수 있습니다."));

        mockMvc.perform(patch("/api/v1/admin/reports/101/resolve")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"HIDE_CONTENT\",\"reason\":\"사유\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void 존재하지_않는_신고면_404를_반환한다() throws Exception {
        when(resolveReportUseCase.execute(999L, 2L, "HIDE_CONTENT", "사유")).thenThrow(
                new ReportResolutionApplicationException(
                        ReportResolutionErrorCode.REPORT_NOT_FOUND, "존재하지 않는 신고입니다."));

        mockMvc.perform(patch("/api/v1/admin/reports/999/resolve")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"HIDE_CONTENT\",\"reason\":\"사유\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("REPORT_NOT_FOUND"));
    }

    @Test
    void 이미_처리된_신고면_409를_반환한다() throws Exception {
        when(resolveReportUseCase.execute(101L, 2L, "HIDE_CONTENT", "사유")).thenThrow(
                new ReportResolutionApplicationException(
                        ReportResolutionErrorCode.ALREADY_HANDLED, "이미 처리된 신고입니다."));

        mockMvc.perform(patch("/api/v1/admin/reports/101/resolve")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"HIDE_CONTENT\",\"reason\":\"사유\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_HANDLED"));
    }

    @Test
    void 인증없는_요청시_401을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/reports/101/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"HIDE_CONTENT\",\"reason\":\"사유\"}"))
                .andExpect(status().isUnauthorized());
    }
}
