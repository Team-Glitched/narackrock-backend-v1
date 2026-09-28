package glitched.adlips.adapter.in.web.community;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.community.PostApplicationException;
import glitched.adlips.application.community.PostBookmarkResult;
import glitched.adlips.application.community.PostErrorCode;
import glitched.adlips.application.community.TogglePostBookmarkUseCase;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PostBookmarkController.class)
@Import(SecurityConfig.class)
class PostBookmarkControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean TogglePostBookmarkUseCase togglePostBookmarkUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    String validToken;

    @BeforeEach
    void setUp() {
        validToken = jwtTokenIssuerAdapter.issue(1L);
        when(accessTokenPort.verify(validToken)).thenReturn(1L);
        when(authenticatedUserResolver.requireUserId("Bearer " + validToken)).thenReturn(1L);
    }

    @Test
    void 게시글_북마크_토글은_200과_북마크_상태를_반환한다() throws Exception {
        when(togglePostBookmarkUseCase.toggle(1L, 10L)).thenReturn(new PostBookmarkResult(10L, true));

        mockMvc.perform(post("/api/v1/posts/10/bookmark").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(10))
                .andExpect(jsonPath("$.data.isBookmarked").value(true));
    }

    @Test
    void 없는_게시글이면_404를_반환한다() throws Exception {
        when(togglePostBookmarkUseCase.toggle(1L, 999L)).thenThrow(
                new PostApplicationException(PostErrorCode.POST_NOT_FOUND, "존재하지 않거나 삭제된 게시글입니다."));

        mockMvc.perform(post("/api/v1/posts/999/bookmark").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("POST_NOT_FOUND"));
    }

    @Test
    void 인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/posts/10/bookmark"))
                .andExpect(status().isUnauthorized());
    }
}
