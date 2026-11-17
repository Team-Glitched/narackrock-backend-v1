package glitched.adlips.adapter.in.web.community;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import glitched.adlips.application.community.GalleryBanPage;
import glitched.adlips.application.community.GetGalleryBansUseCase;
import glitched.adlips.application.community.GetGalleryManagersUseCase;
import glitched.adlips.application.community.GrantGalleryManagerUseCase;
import glitched.adlips.application.community.LiftGalleryBanUseCase;
import glitched.adlips.application.community.ModeratePostCommentDeletionUseCase;
import glitched.adlips.application.community.ModeratePostDeletionUseCase;
import glitched.adlips.application.community.RevokeGalleryManagerUseCase;
import glitched.adlips.application.community.SanctionGalleryUserUseCase;
import glitched.adlips.application.community.port.out.GalleryBanItem;
import glitched.adlips.domain.community.GalleryBanType;
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
    @MockitoBean SanctionGalleryUserUseCase sanctionGalleryUserUseCase;
    @MockitoBean LiftGalleryBanUseCase liftGalleryBanUseCase;
    @MockitoBean GetGalleryBansUseCase getGalleryBansUseCase;
    @MockitoBean ModeratePostDeletionUseCase moderatePostDeletionUseCase;
    @MockitoBean ModeratePostCommentDeletionUseCase moderatePostCommentDeletionUseCase;
    @MockitoBean GetGalleryManagersUseCase getGalleryManagersUseCase;
    @MockitoBean GrantGalleryManagerUseCase grantGalleryManagerUseCase;
    @MockitoBean RevokeGalleryManagerUseCase revokeGalleryManagerUseCase;
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

    @Test
    void 이용정지를_부과하면_201과_제재_ID를_반환한다() throws Exception {
        when(sanctionGalleryUserUseCase.execute(3L, 10L, 1L, "SUSPEND", 7, "도배")).thenReturn(70L);

        mockMvc.perform(post("/api/v1/galleries/10/manage/bans")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"type\":\"SUSPEND\",\"durationDays\":7,\"reason\":\"도배\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.banId").value(70));
    }

    @Test
    void 이미_제재_중이면_409를_반환한다() throws Exception {
        when(sanctionGalleryUserUseCase.execute(3L, 10L, 1L, "EXPEL", null, "광고")).thenThrow(
                new GalleryModerationApplicationException(GalleryModerationErrorCode.ALREADY_BANNED_IN_GALLERY, "중복"));

        mockMvc.perform(post("/api/v1/galleries/10/manage/bans")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"type\":\"EXPEL\",\"reason\":\"광고\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_BANNED_IN_GALLERY"));
    }

    @Test
    void 제재할_수_없는_대상이면_403을_반환한다() throws Exception {
        when(sanctionGalleryUserUseCase.execute(3L, 10L, 9L, "EXPEL", null, "광고")).thenThrow(
                new GalleryModerationApplicationException(GalleryModerationErrorCode.CANNOT_SANCTION_TARGET, "불가"));

        mockMvc.perform(post("/api/v1/galleries/10/manage/bans")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":9,\"type\":\"EXPEL\",\"reason\":\"광고\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 제재를_해제한다() throws Exception {
        mockMvc.perform(patch("/api/v1/galleries/10/manage/bans/70/lift").header("Authorization", bearer))
                .andExpect(status().isOk());

        verify(liftGalleryBanUseCase).execute(3L, 10L, 70L);
    }

    @Test
    void 제재_목록을_반환한다() throws Exception {
        when(getGalleryBansUseCase.execute(3L, 10L, true, null, 20)).thenReturn(new GalleryBanPage(
                List.of(new GalleryBanItem(70L, 1L, 3L, GalleryBanType.SUSPEND, "도배",
                        LocalDateTime.of(2026, 10, 14, 12, 0), null, LocalDateTime.of(2026, 10, 7, 12, 0), true)),
                null, false));

        mockMvc.perform(get("/api/v1/galleries/10/manage/bans")
                        .param("activeOnly", "true")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bans[0].banId").value(70))
                .andExpect(jsonPath("$.data.bans[0].banType").value("SUSPEND"))
                .andExpect(jsonPath("$.data.bans[0].bannedUntil").value("2026-10-14T12:00:00"))
                .andExpect(jsonPath("$.data.bans[0].active").value(true));
    }

    @Test
    void 매니저가_게시글을_삭제한다() throws Exception {
        mockMvc.perform(patch("/api/v1/galleries/10/manage/posts/50/delete")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"광고\"}"))
                .andExpect(status().isOk());

        verify(moderatePostDeletionUseCase).execute(3L, 10L, 50L, "광고");
    }

    @Test
    void 다른_갤러리의_게시글이면_404를_반환한다() throws Exception {
        doThrow(new GalleryModerationApplicationException(GalleryModerationErrorCode.POST_NOT_FOUND, "없음"))
                .when(moderatePostDeletionUseCase).execute(3L, 10L, 50L, "광고");

        mockMvc.perform(patch("/api/v1/galleries/10/manage/posts/50/delete")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"광고\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("POST_NOT_FOUND"));
    }

    @Test
    void 매니저가_댓글을_삭제한다() throws Exception {
        mockMvc.perform(patch("/api/v1/galleries/10/manage/comments/70/delete")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"욕설\"}"))
                .andExpect(status().isOk());

        verify(moderatePostCommentDeletionUseCase).execute(3L, 10L, 70L, "욕설");
    }
}
