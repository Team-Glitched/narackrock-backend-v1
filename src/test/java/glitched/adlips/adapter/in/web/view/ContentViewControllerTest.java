package glitched.adlips.adapter.in.web.view;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.view.ContentViewApplicationException;
import glitched.adlips.application.view.ContentViewErrorCode;
import glitched.adlips.application.view.ContentViewResult;
import glitched.adlips.application.view.ContentViewTarget;
import glitched.adlips.application.view.RecordContentViewUseCase;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContentViewController.class)
@Import({SecurityConfig.class, ContentViewExceptionHandler.class})
class ContentViewControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean RecordContentViewUseCase useCase;
    @MockitoBean ViewIdentityResolver identityResolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    @Test
    void 기기_ID로_숏폼_조회수를_기록하고_202를_반환한다() throws Exception {
        when(identityResolver.resolve(null, "device-id")).thenReturn("device:hash");
        when(useCase.execute(ContentViewTarget.SHORT, 12L, "device:hash"))
                .thenReturn(new ContentViewResult(ContentViewTarget.SHORT, 12L, true));

        mockMvc.perform(post("/api/v1/shorts/12/views")
                        .header("X-Device-Id", "device-id"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.contentId").value(12))
                .andExpect(jsonPath("$.data.counted").value(true));
    }

    @Test
    void 로그인_사용자의_게시글_조회수를_기록한다() throws Exception {
        when(identityResolver.resolve("Bearer token", null)).thenReturn("user:7");
        when(useCase.execute(ContentViewTarget.POST, 501L, "user:7"))
                .thenReturn(new ContentViewResult(ContentViewTarget.POST, 501L, false));

        mockMvc.perform(post("/api/v1/posts/501/views")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.contentId").value(501))
                .andExpect(jsonPath("$.data.counted").value(false));
    }

    @Test
    void 조회자_식별값이_없으면_400을_반환한다() throws Exception {
        when(identityResolver.resolve(null, null)).thenThrow(new ContentViewApplicationException(
                ContentViewErrorCode.VIEWER_ID_REQUIRED,
                "조회자를 식별할 인증 정보 또는 기기 ID가 필요합니다."));

        mockMvc.perform(post("/api/v1/shorts/12/views"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VIEWER_ID_REQUIRED"));
    }

    @Test
    void 콘텐츠가_없으면_404를_반환한다() throws Exception {
        when(identityResolver.resolve(null, "device-id")).thenReturn("device:hash");
        when(useCase.execute(ContentViewTarget.POST, 999L, "device:hash"))
                .thenThrow(new ContentViewApplicationException(
                        ContentViewErrorCode.CONTENT_NOT_FOUND,
                        "존재하지 않거나 조회할 수 없는 콘텐츠입니다."));

        mockMvc.perform(post("/api/v1/posts/999/views")
                        .header("X-Device-Id", "device-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CONTENT_NOT_FOUND"));
    }

    @Test
    void Redis를_사용할_수_없으면_503을_반환한다() throws Exception {
        when(identityResolver.resolve(null, "device-id")).thenReturn("device:hash");
        when(useCase.execute(ContentViewTarget.SHORT, 12L, "device:hash"))
                .thenThrow(new ContentViewApplicationException(
                        ContentViewErrorCode.VIEW_COUNT_UNAVAILABLE,
                        "조회수 집계 서비스를 일시적으로 사용할 수 없습니다."));

        mockMvc.perform(post("/api/v1/shorts/12/views")
                        .header("X-Device-Id", "device-id"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("VIEW_COUNT_UNAVAILABLE"));
    }
}
