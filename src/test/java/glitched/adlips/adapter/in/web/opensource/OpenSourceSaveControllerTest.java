package glitched.adlips.adapter.in.web.opensource;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.opensource.*;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpenSourceSaveController.class)
@Import(SecurityConfig.class)
class OpenSourceSaveControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtTokenIssuerAdapter tokens;
    @MockitoBean OpenSourceSaveUseCase useCase;
    @MockitoBean AuthenticatedUserResolver users;
    @MockitoBean AccessTokenPort accessTokenPort;
    String authorization;

    @BeforeEach void setup() {
        String token = tokens.issue(1L);
        authorization = "Bearer " + token;
        when(accessTokenPort.verify(token)).thenReturn(1L);
        when(users.requireUserId(authorization)).thenReturn(1L);
    }
    @Test void saveReturnsContract() throws Exception {
        when(useCase.save(1L, 10L)).thenReturn(new OpenSourceSaveResult(10L, true));
        mvc.perform(put("/api/v1/open-sources/10/save").header("Authorization", authorization))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.openSourceId").value(10))
            .andExpect(jsonPath("$.data.isSaved").value(true));
    }
    @Test void removeReturnsContract() throws Exception {
        when(useCase.remove(1L, 10L)).thenReturn(new OpenSourceSaveResult(10L, false));
        mvc.perform(delete("/api/v1/open-sources/10/save").header("Authorization", authorization))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.isSaved").value(false));
    }
    @Test void unavailableReturns404() throws Exception {
        when(useCase.save(1L, 10L)).thenThrow(new OpenSourceException(
            OpenSourceException.Code.OPEN_SOURCE_NOT_FOUND, "오픈소스를 찾을 수 없습니다."));
        mvc.perform(put("/api/v1/open-sources/10/save").header("Authorization", authorization))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("OPEN_SOURCE_NOT_FOUND"));
    }
    @Test void malformedIdReturns400() throws Exception {
        mvc.perform(put("/api/v1/open-sources/abc/save").header("Authorization", authorization))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }
    @Test void missingAuthenticationReturns401() throws Exception {
        mvc.perform(put("/api/v1/open-sources/10/save")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/open-sources/10/save")).andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }
}
