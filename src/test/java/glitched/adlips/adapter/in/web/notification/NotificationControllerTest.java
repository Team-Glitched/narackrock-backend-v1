package glitched.adlips.adapter.in.web.notification;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.notification.NotificationInboxUseCase;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter tokenIssuer;
    @MockitoBean NotificationInboxUseCase inbox;
    @MockitoBean AuthenticatedUserResolver resolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    @Test
    void deletesSingleNotificationAndReadNotificationsSeparately() throws Exception {
        String token = tokenIssuer.issue(7L);
        String authorization = "Bearer " + token;
        when(accessTokenPort.verify(token)).thenReturn(7L);
        when(resolver.requireUserId(authorization)).thenReturn(7L);

        mockMvc.perform(delete("/api/v1/notifications/42").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(inbox).delete(7L, 42L);

        mockMvc.perform(delete("/api/v1/notifications/read").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(inbox).deleteRead(7L);
    }
}
