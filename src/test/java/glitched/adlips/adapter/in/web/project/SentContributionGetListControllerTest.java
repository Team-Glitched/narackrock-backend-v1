package glitched.adlips.adapter.in.web.project;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.project.dto.request.SentContributionGetListRequest;
import glitched.adlips.application.project.dto.response.*;
import glitched.adlips.application.project.usecase.SentContributionGetListUseCase;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.global.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SentContributionGetListController.class)
@Import(SecurityConfig.class)
class SentContributionGetListControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter tokenIssuer;
    @MockitoBean SentContributionGetListUseCase useCase;
    @MockitoBean AuthenticatedUserResolver resolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    @Test
    void usesAuthenticatedSenderEvenWhenADifferentUserIdIsSupplied() throws Exception {
        var token = tokenIssuer.issue(7L);
        var authorization = "Bearer " + token;
        when(accessTokenPort.verify(token)).thenReturn(7L);
        when(resolver.requireUserId(authorization)).thenReturn(7L);
        when(useCase.execute(new SentContributionGetListRequest(7L, 30L, 20)))
                .thenReturn(new SentContributionGetListResponse(List.of(), new ProjectContributionGetListResponse.Page(null, false, 20)));
        mockMvc.perform(get("/api/v1/users/me/contributions?cursor=30&size=20&userId=999")
                .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.page.hasMore").value(false));
        verify(useCase).execute(new SentContributionGetListRequest(7L, 30L, 20));
    }

    @Test
    void anonymousUsersCannotReadSentHistory() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/contributions"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }
}
