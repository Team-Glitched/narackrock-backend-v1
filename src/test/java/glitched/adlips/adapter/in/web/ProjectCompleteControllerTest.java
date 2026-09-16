package glitched.adlips.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.project.ProjectCompleteController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.project.dto.response.ProjectCompleteResponse;
import glitched.adlips.application.project.usecase.ProjectCompleteUseCase;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.domain.project.ProjectStatus;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectCompleteController.class)
@Import(SecurityConfig.class)
class ProjectCompleteControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter tokenIssuer;
    @MockitoBean ProjectCompleteUseCase useCase;
    @MockitoBean AuthenticatedUserResolver resolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    @Test
    void completesPublicProject() throws Exception {
        String token = tokenIssuer.issue(1L);
        String authorization = "Bearer " + token;
        when(accessTokenPort.verify(token)).thenReturn(1L);
        when(resolver.requireUserId(authorization)).thenReturn(1L);
        when(useCase.execute(10L, 1L))
                .thenReturn(new ProjectCompleteResponse(10L, ProjectStatus.COMPLETED, true));

        mockMvc.perform(post("/api/v1/projects/10/complete")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(10))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.isPublic").value(true));
    }
}
