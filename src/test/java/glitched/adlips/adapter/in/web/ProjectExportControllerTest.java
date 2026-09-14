package glitched.adlips.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.project.ProjectExportController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.ProjectExportGetRequest;
import glitched.adlips.application.project.dto.response.ProjectExportGetResponse;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.project.usecase.ProjectExportGetUseCase;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.domain.project.ExportStatus;
import glitched.adlips.global.config.SecurityConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectExportController.class)
@Import(SecurityConfig.class)
class ProjectExportControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean ProjectExportGetUseCase useCase;
    @MockitoBean AuthenticatedUserResolver resolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    private String authorization;

    @BeforeEach
    void setUp() {
        String token = jwtTokenIssuerAdapter.issue(1L);
        authorization = "Bearer " + token;
        when(accessTokenPort.verify(token)).thenReturn(1L);
        when(resolver.requireUserId(authorization)).thenReturn(1L);
    }

    @Test
    void returnsExportStatus() throws Exception {
        var request = new ProjectExportGetRequest(10L, 20L, 1L);
        var completedAt = LocalDateTime.of(2026, 9, 14, 10, 0);
        when(useCase.execute(request)).thenReturn(new ProjectExportGetResponse(
                10L, 20L, new ProjectVersionResponse(1, 1, "v1.1"),
                801L, "/files/audio/mix.wav", 901L, null, null,
                701L, "/files/images/album.png", 32_000, ExportStatus.COMPLETED,
                null, LocalDateTime.of(2026, 9, 14, 9, 59), completedAt));

        mockMvc.perform(get("/api/v1/projects/10/exports/20")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exportId").value(20))
                .andExpect(jsonPath("$.data.projectVersion.display").value("v1.1"))
                .andExpect(jsonPath("$.data.mixedAudioUrl").value("/files/audio/mix.wav"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").value("2026-09-14T10:00:00"));
    }

    @Test
    void returnsNotFoundForUnknownExport() throws Exception {
        var request = new ProjectExportGetRequest(10L, 99L, 1L);
        when(useCase.execute(request)).thenThrow(new ProjectApplicationException(
                ProjectErrorCode.EXPORT_NOT_FOUND, "해당 프로젝트의 Export를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/projects/10/exports/99")
                        .header("Authorization", authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EXPORT_NOT_FOUND"));
    }

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/projects/10/exports/20"))
                .andExpect(status().isUnauthorized());
    }
}
