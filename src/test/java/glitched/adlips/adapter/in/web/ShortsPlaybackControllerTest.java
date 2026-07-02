package glitched.adlips.adapter.in.web;

import glitched.adlips.adapter.in.web.shorts.ShortsPlaybackController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.shorts.ShortPlaybackApplicationException;
import glitched.adlips.application.shorts.ShortPlaybackErrorCode;
import glitched.adlips.application.shorts.ShortPlaybackResult;
import glitched.adlips.application.shorts.ToggleShortPlaybackUseCase;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShortsPlaybackController.class)
@Import(SecurityConfig.class)
class ShortsPlaybackControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean ToggleShortPlaybackUseCase toggleShortPlaybackUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;

    String validToken;

    @BeforeEach
    void setUp() {
        validToken = jwtTokenIssuerAdapter.issue(1L);
        when(authenticatedUserResolver.requireUserId("Bearer " + validToken)).thenReturn(1L);
    }

    @Test
    void 유효한_요청시_200과_결과를_반환한다() throws Exception {
        Instant fixedTime = Instant.parse("2024-01-15T10:30:45Z");
        when(toggleShortPlaybackUseCase.toggle(eq(12L), eq(true), eq(15.5)))
                .thenReturn(new ShortPlaybackResult(12L, false, 15.5, fixedTime));

        mockMvc.perform(patch("/api/v1/shorts/12/playback/toggle")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPlaying\":true,\"currentTime\":15.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.data.shortId").value(12))
                .andExpect(jsonPath("$.data.shortformId").doesNotExist())
                .andExpect(jsonPath("$.data.isPlaying").value(false))
                .andExpect(jsonPath("$.data.pausedAt").value(15.5))
                .andExpect(jsonPath("$.data.timestamp").value(fixedTime.toString()));
    }

    @Test
    void 일시정지_상태에서_요청하면_재생_상태를_반환한다() throws Exception {
        Instant fixedTime = Instant.parse("2024-01-15T10:30:45Z");
        when(toggleShortPlaybackUseCase.toggle(eq(12L), eq(false), eq(15.5)))
                .thenReturn(new ShortPlaybackResult(12L, true, 15.5, fixedTime));

        mockMvc.perform(patch("/api/v1/shorts/12/playback/toggle")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPlaying\":false,\"currentTime\":15.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPlaying").value(true));
    }

    @Test
    void 존재하지_않는_숏폼이면_404를_반환한다() throws Exception {
        when(toggleShortPlaybackUseCase.toggle(eq(999L), eq(true), eq(15.5)))
                .thenThrow(new ShortPlaybackApplicationException(
                        ShortPlaybackErrorCode.SHORT_NOT_FOUND, "존재하지 않는 숏폼입니다."));

        mockMvc.perform(patch("/api/v1/shorts/999/playback/toggle")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPlaying\":true,\"currentTime\":15.5}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SHORT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 숏폼입니다."));
    }

    @Test
    void currentTime이_음수이면_400을_반환한다() throws Exception {
        when(toggleShortPlaybackUseCase.toggle(eq(12L), eq(true), eq(-1.0)))
                .thenThrow(new ShortPlaybackApplicationException(
                        ShortPlaybackErrorCode.INVALID_CURRENT_TIME, "currentTime은 0 이상의 숫자여야 합니다."));

        mockMvc.perform(patch("/api/v1/shorts/12/playback/toggle")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPlaying\":true,\"currentTime\":-1.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CURRENT_TIME"));
    }

    @Test
    void 인증없는_요청시_401을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/shorts/12/playback/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPlaying\":true,\"currentTime\":15.5}"))
                .andExpect(status().isUnauthorized());
    }
}
