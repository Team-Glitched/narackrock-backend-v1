package glitched.adlips.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.shorts.ShortReportController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.shorts.ShortReportApplicationException;
import glitched.adlips.application.shorts.ShortReportErrorCode;
import glitched.adlips.application.shorts.ShortReportResult;
import glitched.adlips.application.shorts.SubmitShortReportUseCase;
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

@WebMvcTest(ShortReportController.class)
@Import(SecurityConfig.class)
class ShortReportControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean SubmitShortReportUseCase submitShortReportUseCase;
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
    void 유효한_요청시_201과_신고_결과를_반환한다() throws Exception {
        when(submitShortReportUseCase.execute(12L, 1L, "COPYRIGHT", "저작권 침해가 의심되는 음원입니다."))
                .thenReturn(new ShortReportResult(31L, 12L));

        mockMvc.perform(post("/api/v1/shorts/12/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"COPYRIGHT\",\"description\":\"저작권 침해가 의심되는 음원입니다.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("숏폼 신고가 접수되었습니다."))
                .andExpect(jsonPath("$.data.reportId").value(31))
                .andExpect(jsonPath("$.data.targetType").value("SHORT"))
                .andExpect(jsonPath("$.data.targetId").value(12));
    }

    @Test
    void 본인_숏폼이면_400을_반환한다() throws Exception {
        when(submitShortReportUseCase.execute(12L, 1L, "COPYRIGHT", "설명")).thenThrow(
                new ShortReportApplicationException(
                        ShortReportErrorCode.CANNOT_REPORT_OWN_SHORT, "본인이 작곡한 숏폼은 신고할 수 없습니다."));

        mockMvc.perform(post("/api/v1/shorts/12/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"COPYRIGHT\",\"description\":\"설명\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CANNOT_REPORT_OWN_SHORT"));
    }

    @Test
    void 존재하지_않는_숏폼이면_404를_반환한다() throws Exception {
        when(submitShortReportUseCase.execute(999L, 1L, "COPYRIGHT", "설명")).thenThrow(
                new ShortReportApplicationException(
                        ShortReportErrorCode.SHORT_NOT_FOUND, "존재하지 않는 숏폼입니다."));

        mockMvc.perform(post("/api/v1/shorts/999/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"COPYRIGHT\",\"description\":\"설명\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SHORT_NOT_FOUND"));
    }

    @Test
    void 이미_신고했으면_409를_반환한다() throws Exception {
        when(submitShortReportUseCase.execute(12L, 1L, "COPYRIGHT", "설명")).thenThrow(
                new ShortReportApplicationException(
                        ShortReportErrorCode.ALREADY_REPORTED, "이미 신고한 숏폼입니다."));

        mockMvc.perform(post("/api/v1/shorts/12/reports")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"COPYRIGHT\",\"description\":\"설명\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_REPORTED"));
    }

    @Test
    void 인증없는_요청시_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/shorts/12/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"COPYRIGHT\",\"description\":\"설명\"}"))
                .andExpect(status().isUnauthorized());
    }
}
