package glitched.adlips.adapter.in.web.community;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.community.GalleryProposalApplicationException;
import glitched.adlips.application.community.GalleryProposalErrorCode;
import glitched.adlips.application.community.GetMyGalleryProposalsUseCase;
import glitched.adlips.application.community.SubmitGalleryProposalUseCase;
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

@WebMvcTest(GalleryProposalController.class)
@Import(SecurityConfig.class)
class GalleryProposalControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean SubmitGalleryProposalUseCase submitGalleryProposalUseCase;
    @MockitoBean GetMyGalleryProposalsUseCase getMyGalleryProposalsUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    String bearer;

    @BeforeEach
    void setUp() {
        String token = jwtTokenIssuerAdapter.issue(1L);
        bearer = "Bearer " + token;
        when(accessTokenPort.verify(token)).thenReturn(1L);
        when(authenticatedUserResolver.requireUserId(bearer)).thenReturn(1L);
    }

    @Test
    void 개설_요청을_생성하면_201과_요청_ID를_반환한다() throws Exception {
        when(submitGalleryProposalUseCase.execute(1L, "재즈", "재즈 이야기")).thenReturn(50L);

        mockMvc.perform(post("/api/v1/gallery-proposals")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"재즈\",\"description\":\"재즈 이야기\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.proposalId").value(50))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void 인증이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/gallery-proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"재즈\",\"description\":\"설명\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 중복_이름이면_409와_에러코드를_반환한다() throws Exception {
        when(submitGalleryProposalUseCase.execute(1L, "기타", "설명")).thenThrow(
                new GalleryProposalApplicationException(
                        GalleryProposalErrorCode.GALLERY_NAME_DUPLICATED, "이미 존재하는 갤러리 이름입니다."));

        mockMvc.perform(post("/api/v1/gallery-proposals")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"기타\",\"description\":\"설명\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("GALLERY_NAME_DUPLICATED"));
    }

    @Test
    void 입력_오류면_400을_반환한다() throws Exception {
        when(submitGalleryProposalUseCase.execute(1L, "재", "설명")).thenThrow(
                new GalleryProposalApplicationException(GalleryProposalErrorCode.VALIDATION_ERROR, "입력 오류"));

        mockMvc.perform(post("/api/v1/gallery-proposals")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"재\",\"description\":\"설명\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void 정지_사용자면_403을_반환한다() throws Exception {
        when(submitGalleryProposalUseCase.execute(1L, "재즈", "설명")).thenThrow(
                new GalleryProposalApplicationException(GalleryProposalErrorCode.BANNED_USER_ACCESS, "정지"));

        mockMvc.perform(post("/api/v1/gallery-proposals")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"재즈\",\"description\":\"설명\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 내_개설_요청_목록을_반환한다() throws Exception {
        when(getMyGalleryProposalsUseCase.execute(1L)).thenReturn(List.of(new GalleryProposalItem(
                50L, 1L, "재즈", "재즈 이야기", GalleryProposalStatus.REJECTED, "중복 주제",
                LocalDateTime.of(2026, 10, 7, 12, 0), LocalDateTime.of(2026, 10, 8, 9, 0))));

        mockMvc.perform(get("/api/v1/gallery-proposals/me").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].proposalId").value(50))
                .andExpect(jsonPath("$.data[0].name").value("재즈"))
                .andExpect(jsonPath("$.data[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.data[0].rejectionReason").value("중복 주제"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-10-07T12:00:00"))
                .andExpect(jsonPath("$.data[0].reviewedAt").value("2026-10-08T09:00:00"));
    }
}
