package glitched.adlips.adapter.in.web;

import glitched.adlips.adapter.in.web.shorts.ShortsReactionController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.shorts.ShortDislikeResult;
import glitched.adlips.application.shorts.ShortLikeResult;
import glitched.adlips.application.shorts.ToggleShortDislikeUseCase;
import glitched.adlips.application.shorts.ToggleShortLikeUseCase;
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

@WebMvcTest(ShortsReactionController.class)
@Import(SecurityConfig.class)
class ShortsReactionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean ToggleShortLikeUseCase toggleShortLikeUseCase;
    @MockitoBean ToggleShortDislikeUseCase toggleShortDislikeUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;

    String validToken;

    @BeforeEach
    void setUp() {
        validToken = jwtTokenIssuerAdapter.issue(1L);
        when(authenticatedUserResolver.requireUserId("Bearer " + validToken)).thenReturn(1L);
    }

    @Test
    void 좋아요_토글_성공시_200과_결과를_반환한다() throws Exception {
        when(toggleShortLikeUseCase.toggle(eq(1L), eq(12L)))
                .thenReturn(new ShortLikeResult(12L, true, 129));

        mockMvc.perform(post("/api/v1/shorts/12/like")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("숏폼 좋아요 상태가 변경되었습니다."))
                .andExpect(jsonPath("$.data.shortId").value(12))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(129));
    }

    @Test
    void 좋아요_취소시_isLiked가_false로_반환된다() throws Exception {
        when(toggleShortLikeUseCase.toggle(eq(1L), eq(12L)))
                .thenReturn(new ShortLikeResult(12L, false, 128));

        mockMvc.perform(post("/api/v1/shorts/12/like")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(128));
    }

    @Test
    void 좋아요_인증없는_요청시_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/shorts/12/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 싫어요_토글_성공시_200과_결과를_반환한다() throws Exception {
        when(toggleShortDislikeUseCase.toggle(eq(1L), eq(12L)))
                .thenReturn(new ShortDislikeResult(12L, true, 129));

        mockMvc.perform(post("/api/v1/shorts/12/dislike")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("숏폼 싫어요 상태가 변경되었습니다."))
                .andExpect(jsonPath("$.data.shortId").value(12))
                .andExpect(jsonPath("$.data.isDisliked").value(true))
                .andExpect(jsonPath("$.data.dislikeCount").value(129));
    }

    @Test
    void 싫어요_취소시_isDisliked가_false로_반환된다() throws Exception {
        when(toggleShortDislikeUseCase.toggle(eq(1L), eq(12L)))
                .thenReturn(new ShortDislikeResult(12L, false, 128));

        mockMvc.perform(post("/api/v1/shorts/12/dislike")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDisliked").value(false))
                .andExpect(jsonPath("$.data.dislikeCount").value(128));
    }

    @Test
    void 싫어요_인증없는_요청시_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/shorts/12/dislike"))
                .andExpect(status().isUnauthorized());
    }
}
