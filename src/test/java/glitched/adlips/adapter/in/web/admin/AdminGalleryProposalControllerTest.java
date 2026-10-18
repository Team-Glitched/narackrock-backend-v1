package glitched.adlips.adapter.in.web.admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.community.ApproveGalleryProposalUseCase;
import glitched.adlips.application.community.GalleryProposalApplicationException;
import glitched.adlips.application.community.GalleryProposalErrorCode;
import glitched.adlips.application.community.GalleryProposalPage;
import glitched.adlips.application.community.GalleryProposalReviewResult;
import glitched.adlips.application.community.GetGalleryProposalsUseCase;
import glitched.adlips.application.community.RejectGalleryProposalUseCase;
import glitched.adlips.application.community.port.out.GalleryProposalItem;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.domain.community.GalleryProposalStatus;
import glitched.adlips.global.config.SecurityConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminGalleryProposalController.class)
@Import(SecurityConfig.class)
class AdminGalleryProposalControllerTest {

    private static final LocalDateTime REVIEWED_AT = LocalDateTime.of(2026, 10, 7, 12, 0);

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean GetGalleryProposalsUseCase getGalleryProposalsUseCase;
    @MockitoBean ApproveGalleryProposalUseCase approveGalleryProposalUseCase;
    @MockitoBean RejectGalleryProposalUseCase rejectGalleryProposalUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    String bearer;

    @BeforeEach
    void setUp() {
        String token = jwtTokenIssuerAdapter.issue(9L);
        bearer = "Bearer " + token;
        when(accessTokenPort.verify(token)).thenReturn(9L);
        when(authenticatedUserResolver.requireUserId(bearer)).thenReturn(9L);
    }

    @Test
    void 개설_요청_목록을_커서와_함께_반환한다() throws Exception {
        when(getGalleryProposalsUseCase.execute(9L, "PENDING", null, 20)).thenReturn(new GalleryProposalPage(
                List.of(new GalleryProposalItem(50L, 1L, "재즈", "설명", GalleryProposalStatus.PENDING, null,
                        REVIEWED_AT, null)),
                50L, true));

        mockMvc.perform(get("/api/v1/admin/gallery-proposals")
                        .param("status", "PENDING")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.proposals[0].proposalId").value(50))
                .andExpect(jsonPath("$.data.proposals[0].proposerId").value(1))
                .andExpect(jsonPath("$.data.nextCursor").value(50))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void 승인하면_생성된_갤러리_ID를_반환한다() throws Exception {
        when(approveGalleryProposalUseCase.execute(50L, 9L)).thenReturn(
                new GalleryProposalReviewResult(50L, GalleryProposalStatus.APPROVED, 10L, REVIEWED_AT));

        mockMvc.perform(patch("/api/v1/admin/gallery-proposals/50/approve").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.proposalId").value(50))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.galleryId").value(10))
                .andExpect(jsonPath("$.data.reviewedAt").value("2026-10-07T12:00:00"));
    }

    @Test
    void 반려하면_반려_상태를_반환한다() throws Exception {
        when(rejectGalleryProposalUseCase.execute(50L, 9L, "중복 주제")).thenReturn(
                new GalleryProposalReviewResult(50L, GalleryProposalStatus.REJECTED, null, REVIEWED_AT));

        mockMvc.perform(patch("/api/v1/admin/gallery-proposals/50/reject")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"중복 주제\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void 어드민이_아니면_403을_반환한다() throws Exception {
        when(approveGalleryProposalUseCase.execute(50L, 9L)).thenThrow(new GalleryProposalApplicationException(
                GalleryProposalErrorCode.FORBIDDEN, "관리자만 처리할 수 있습니다."));

        mockMvc.perform(patch("/api/v1/admin/gallery-proposals/50/approve").header("Authorization", bearer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void 이미_검토된_요청이면_409를_반환한다() throws Exception {
        when(approveGalleryProposalUseCase.execute(50L, 9L)).thenThrow(new GalleryProposalApplicationException(
                GalleryProposalErrorCode.PROPOSAL_ALREADY_REVIEWED, "이미 검토된 요청입니다."));

        mockMvc.perform(patch("/api/v1/admin/gallery-proposals/50/approve").header("Authorization", bearer))
                .andExpect(status().isConflict());
    }

    @Test
    void 인증이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/gallery-proposals"))
                .andExpect(status().isUnauthorized());
    }
}
