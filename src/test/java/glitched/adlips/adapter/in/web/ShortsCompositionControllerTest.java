package glitched.adlips.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.shorts.ShortsCompositionController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.shorts.ShortsCompositionApplicationException;
import glitched.adlips.application.shorts.ShortsCompositionEntryUseCase;
import glitched.adlips.application.shorts.ShortsCompositionErrorCode;
import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.domain.project.ProjectStatus;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShortsCompositionController.class)
@Import(SecurityConfig.class)
class ShortsCompositionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean ShortsCompositionEntryUseCase shortsCompositionEntryUseCase;
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
    void 유효한_요청시_200과_작곡_진입_정보를_반환한다() throws Exception {
        when(shortsCompositionEntryUseCase.execute(12L)).thenReturn(new ShortsCompositionQueryItem(
                12L, 8L, "밤하늘 위 멜로디", ProjectStatus.IN_PROGRESS, null));

        mockMvc.perform(get("/api/v1/shorts/12/composition")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("작곡 화면 진입 정보 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.shortId").value(12))
                .andExpect(jsonPath("$.data.projectId").value(8))
                .andExpect(jsonPath("$.data.compositionUrl").value("/composition/projects/8"))
                .andExpect(jsonPath("$.data.title").value("밤하늘 위 멜로디"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void 존재하지_않는_숏폼이면_404를_반환한다() throws Exception {
        when(shortsCompositionEntryUseCase.execute(999L)).thenThrow(
                new ShortsCompositionApplicationException(
                        ShortsCompositionErrorCode.SHORT_NOT_FOUND, "숏폼을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/shorts/999/composition")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SHORT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("숏폼을 찾을 수 없습니다."));
    }

    @Test
    void 연결된_프로젝트가_없으면_404를_반환한다() throws Exception {
        when(shortsCompositionEntryUseCase.execute(12L)).thenThrow(
                new ShortsCompositionApplicationException(
                        ShortsCompositionErrorCode.PROJECT_NOT_LINKED, "연결된 작곡 프로젝트를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/shorts/12/composition")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PROJECT_NOT_LINKED"));
    }

    @Test
    void 인증없는_요청시_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/shorts/12/composition"))
                .andExpect(status().isUnauthorized());
    }
}
