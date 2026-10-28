package glitched.adlips.adapter.in.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import glitched.adlips.adapter.in.web.project.EditorDraftController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.project.*;
import glitched.adlips.application.project.usecase.*;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.domain.project.*;
import glitched.adlips.global.config.SecurityConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EditorDraftController.class)
@Import(SecurityConfig.class)
class EditorDraftControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtTokenIssuerAdapter tokens;
    @MockitoBean EditorDraftUseCase drafts;
    @MockitoBean EditorDraftWorkflowUseCase workflow;
    @MockitoBean AuthenticatedUserResolver resolver;
    @MockitoBean AccessTokenPort accessTokens;
    String token;
    final UUID id = UUID.randomUUID();

    @BeforeEach void authenticate() {
        token = tokens.issue(1L);
        when(accessTokens.verify(token)).thenReturn(1L);
        when(resolver.requireUserId("Bearer " + token)).thenReturn(1L);
    }

    @Test void usesAuthenticatedIdentityAndReturnsPrivateDrafts() throws Exception {
        when(drafts.list(10, 1)).thenReturn(List.of(EditorDraft.create(id, 1, 10, 20, null,
                new EditorDraftSnapshot(0, 480, List.of()), null, UUID.randomUUID(), Instant.now())));
        mvc.perform(get("/api/v1/projects/10/editor-drafts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].ownerId").value(1));
    }

    @Test void mapsStaleUpdatesTo409AndIgnoresBodyUserId() throws Exception {
        when(drafts.save(eq(10L), eq(20L), eq(id), eq(1L), any())).thenThrow(
                new ProjectApplicationException(ProjectErrorCode.EDITOR_DRAFT_CONFLICT, "충돌"));
        mvc.perform(put("/api/v1/projects/10/tracks/20/editor-drafts/" + id)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId":999,"expectedRevision":0,"mutationId":"%s","targetClipId":null,
                        "content":{"startTick":0,"durationTick":480,"midiNotes":[]},"baseClip":null}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("EDITOR_DRAFT_CONFLICT"));
    }

    @Test void doesNotExposeAnotherUsersDraft() throws Exception {
        when(drafts.get(10, id, 1)).thenThrow(new ProjectApplicationException(
                ProjectErrorCode.EDITOR_DRAFT_NOT_FOUND, "없음"));
        mvc.perform(get("/api/v1/projects/10/editor-drafts/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/projects/10/editor-drafts")).andExpect(status().isUnauthorized());
    }
}
