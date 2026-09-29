package glitched.adlips.adapter.in.web.project;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.project.dto.response.ProjectContributionNotificationResponse;
import glitched.adlips.application.project.usecase.ProjectContributionNotificationUseCase;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.global.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectContributionNotificationController.class)
@Import(SecurityConfig.class)
class ProjectContributionNotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter tokenIssuer;
    @MockitoBean ProjectContributionNotificationUseCase useCase;
    @MockitoBean AuthenticatedUserResolver resolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    @Test
    void returnsContributionNotificationsWithoutExpiringAValidLogin() throws Exception {
        String token = tokenIssuer.issue(1L);
        String authorization = "Bearer " + token;
        when(accessTokenPort.verify(token)).thenReturn(1L);
        when(resolver.requireUserId(authorization)).thenReturn(1L);
        when(useCase.execute(1L)).thenReturn(new ProjectContributionNotificationResponse(List.of()));

        mockMvc.perform(get("/api/v1/notifications/contributions")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }
}
