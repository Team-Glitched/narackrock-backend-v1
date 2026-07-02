package glitched.adlips.adapter.in.web;

import glitched.adlips.adapter.in.web.shorts.ShortsBookmarkController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.shorts.ShortBookmarkResult;
import glitched.adlips.application.shorts.ShortBookmarkApplicationException;
import glitched.adlips.application.shorts.ShortBookmarkErrorCode;
import glitched.adlips.application.shorts.ToggleShortBookmarkUseCase;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShortsBookmarkController.class)
@Import(SecurityConfig.class)
class ShortsBookmarkControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean ToggleShortBookmarkUseCase toggleShortBookmarkUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;

    String validToken;

    @BeforeEach
    void setUp() {
        validToken = jwtTokenIssuerAdapter.issue(1L);
        when(authenticatedUserResolver.requireUserId("Bearer " + validToken)).thenReturn(1L);
    }

    @Test
    void 북마크_토글_성공시_200과_결과를_반환한다() throws Exception {
        when(toggleShortBookmarkUseCase.toggle(eq(1L), eq(12L)))
                .thenReturn(new ShortBookmarkResult(12L, true));

        mockMvc.perform(post("/api/v1/shorts/12/bookmark")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("북마크 상태가 변경되었습니다."))
                .andExpect(jsonPath("$.data.shortId").value(12))
                .andExpect(jsonPath("$.data.isBookmarked").value(true));
    }

    @Test
    void 북마크_취소시_isBookmarked가_false로_반환된다() throws Exception {
        when(toggleShortBookmarkUseCase.toggle(eq(1L), eq(12L)))
                .thenReturn(new ShortBookmarkResult(12L, false));

        mockMvc.perform(post("/api/v1/shorts/12/bookmark")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isBookmarked").value(false));
    }

    @Test
    void 북마크_인증없는_요청시_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/shorts/12/bookmark"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 활성_숏폼이_없으면_404를_반환한다() throws Exception {
        when(toggleShortBookmarkUseCase.toggle(eq(1L), eq(999L)))
                .thenThrow(new ShortBookmarkApplicationException(
                        ShortBookmarkErrorCode.SHORT_NOT_FOUND,
                        "숏폼을 찾을 수 없습니다."));

        mockMvc.perform(post("/api/v1/shorts/999/bookmark")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SHORT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("숏폼을 찾을 수 없습니다."));
    }
}
