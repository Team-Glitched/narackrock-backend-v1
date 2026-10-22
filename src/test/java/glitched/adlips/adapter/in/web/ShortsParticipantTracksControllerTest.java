package glitched.adlips.adapter.in.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import glitched.adlips.adapter.in.web.shorts.ShortsParticipantTracksController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.shorts.*;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.global.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShortsParticipantTracksController.class)
@Import(SecurityConfig.class)
class ShortsParticipantTracksControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtTokenIssuerAdapter tokens;
    @MockitoBean GetShortsParticipantTracksUseCase useCase;
    @MockitoBean AuthenticatedUserResolver users;
    @MockitoBean AccessTokenPort accessTokens;

    @Test
    void authenticatedListenerCanReadWithoutProjectMembership() throws Exception {
        String token = tokens.issue(1L);
        when(accessTokens.verify(token)).thenReturn(1L);
        when(users.requireUserId("Bearer " + token)).thenReturn(1L);
        when(useCase.execute(12L)).thenReturn(new ShortsParticipantTracksResult(12L,
                new ShortsParticipantTracksResult.ProjectTiming(120, 480, 60000), List.of()));
        mvc.perform(get("/api/v1/shorts/12/participant-tracks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.shortId").value(12))
                .andExpect(jsonPath("$.data.project.bpm").value(120))
                .andExpect(jsonPath("$.data.tracks").isArray())
                .andExpect(jsonPath("$.data.viewerPermission").doesNotExist());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/v1/shorts/12/participant-tracks")).andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }

    @Test
    void unavailableShortReturns404() throws Exception {
        String token = tokens.issue(1L);
        when(accessTokens.verify(token)).thenReturn(1L);
        when(useCase.execute(99L)).thenThrow(new ShortsComposersApplicationException(
                ShortsComposersErrorCode.SHORTS_NOT_FOUND, "곡을 찾을 수 없습니다."));
        mvc.perform(get("/api/v1/shorts/99/participant-tracks").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("SHORTS_NOT_FOUND"));
    }
}
