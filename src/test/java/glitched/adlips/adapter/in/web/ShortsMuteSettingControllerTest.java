package glitched.adlips.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.shorts.ShortsMuteSettingController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.usecase.ProfileMuteToggleUseCase;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShortsMuteSettingController.class)
@Import(SecurityConfig.class)
class ShortsMuteSettingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean ProfileMuteToggleUseCase profileMuteToggleUseCase;
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
    void 유효한_요청시_200과_음소거_상태를_반환한다() throws Exception {
        when(profileMuteToggleUseCase.execute(1L)).thenReturn(true);

        mockMvc.perform(patch("/api/v1/shorts/me/settings/mute")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isMuted").value(true));
    }

    @Test
    void 다시_호출하면_해제된_상태를_반환한다() throws Exception {
        when(profileMuteToggleUseCase.execute(1L)).thenReturn(false);

        mockMvc.perform(patch("/api/v1/shorts/me/settings/mute")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isMuted").value(false));
    }

    @Test
    void 프로필이_없으면_404를_반환한다() throws Exception {
        when(profileMuteToggleUseCase.execute(1L)).thenThrow(
                new UserApplicationException(UserErrorCode.PROFILE_NOT_FOUND, "존재하지 않는 사용자 프로필입니다."));

        mockMvc.perform(patch("/api/v1/shorts/me/settings/mute")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void 인증없는_요청시_401을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/shorts/me/settings/mute"))
                .andExpect(status().isUnauthorized());
    }
}
