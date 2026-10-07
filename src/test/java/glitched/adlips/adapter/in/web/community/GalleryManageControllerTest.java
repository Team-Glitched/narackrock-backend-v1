package glitched.adlips.adapter.in.web.community;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.community.GalleryModerationApplicationException;
import glitched.adlips.application.community.GalleryModerationErrorCode;
import glitched.adlips.application.community.GalleryModerationLogPage;
import glitched.adlips.application.community.GalleryRoleResult;
import glitched.adlips.application.community.GetGalleryModerationLogsUseCase;
import glitched.adlips.application.community.GetMyGalleryRoleUseCase;
import glitched.adlips.application.community.UpdateGalleryUseCase;
import glitched.adlips.application.community.port.out.GalleryModerationLogItem;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.domain.community.GalleryModerationActionType;
import glitched.adlips.domain.community.GalleryRole;
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

@WebMvcTest(GalleryManageController.class)
@Import(SecurityConfig.class)
class GalleryManageControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean GetMyGalleryRoleUseCase getMyGalleryRoleUseCase;
    @MockitoBean UpdateGalleryUseCase updateGalleryUseCase;
    @MockitoBean GetGalleryModerationLogsUseCase getGalleryModerationLogsUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    String bearer;

    @BeforeEach
    void setUp() {
        String token = jwtTokenIssuerAdapter.issue(3L);
        bearer = "Bearer " + token;
        when(accessTokenPort.verify(token)).thenReturn(3L);
        when(authenticatedUserResolver.requireUserId(bearer)).thenReturn(3L);
    }

    @Test
    void 내_역할을_반환한다() throws Exception {
        when(getMyGalleryRoleUseCase.execute(3L, 10L))
                .thenReturn(new GalleryRoleResult(10L, GalleryRole.MANAGER, false, true));

        mockMvc.perform(get("/api/v1/galleries/10/manage/me").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.galleryId").value(10))
                .andExpect(jsonPath("$.data.galleryRole").value("MANAGER"))
                .andExpect(jsonPath("$.data.isAdmin").value(false))
                .andExpect(jsonPath("$.data.canManage").value(true));
    }

    @Test
    void 역할이_없으면_galleryRole이_null로_내려간다() throws Exception {
        when(getMyGalleryRoleUseCase.execute(3L, 10L)).thenReturn(new GalleryRoleResult(10L, null, false, false));

        mockMvc.perform(get("/api/v1/galleries/10/manage/me").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.galleryRole").isEmpty())
                .andExpect(jsonPath("$.data.canManage").value(false));
    }

    @Test
    void 갤러리_소개를_수정한다() throws Exception {
        mockMvc.perform(patch("/api/v1/galleries/10/manage")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"새 소개\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(updateGalleryUseCase).execute(3L, 10L, "새 소개");
    }

    @Test
    void 권한이_없으면_403을_반환한다() throws Exception {
        doThrow(new GalleryModerationApplicationException(GalleryModerationErrorCode.FORBIDDEN, "권한 없음"))
                .when(updateGalleryUseCase).execute(3L, 10L, "새 소개");

        mockMvc.perform(patch("/api/v1/galleries/10/manage")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"새 소개\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void 갤러리가_없으면_404를_반환한다() throws Exception {
        when(getMyGalleryRoleUseCase.execute(3L, 404L)).thenThrow(new GalleryModerationApplicationException(
                GalleryModerationErrorCode.GALLERY_NOT_FOUND, "없음"));

        mockMvc.perform(get("/api/v1/galleries/404/manage/me").header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    void 관리_이력을_반환한다() throws Exception {
        when(getGalleryModerationLogsUseCase.execute(3L, 10L, null, 20)).thenReturn(new GalleryModerationLogPage(
                List.of(new GalleryModerationLogItem(5L, GalleryModerationActionType.DELETE_POST, 3L, 1L, 99L, "광고",
                        LocalDateTime.of(2026, 10, 7, 12, 0))),
                null, false));

        mockMvc.perform(get("/api/v1/galleries/10/manage/logs").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logs[0].logId").value(5))
                .andExpect(jsonPath("$.data.logs[0].actionType").value("DELETE_POST"))
                .andExpect(jsonPath("$.data.logs[0].targetId").value(99))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void 인증이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/galleries/10/manage/me"))
                .andExpect(status().isUnauthorized());
    }
}
