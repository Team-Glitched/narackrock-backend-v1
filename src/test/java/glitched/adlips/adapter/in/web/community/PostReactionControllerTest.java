package glitched.adlips.adapter.in.web.community;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.community.PostApplicationException;
import glitched.adlips.application.community.PostErrorCode;
import glitched.adlips.application.community.PostReactionResult;
import glitched.adlips.application.community.TogglePostReactionUseCase;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.domain.reaction.ReactionType;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PostReactionController.class)
@Import(SecurityConfig.class)
class PostReactionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean TogglePostReactionUseCase togglePostReactionUseCase;
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
    void 게시글_좋아요_토글은_200과_반응_상태를_반환한다() throws Exception {
        when(togglePostReactionUseCase.toggle(1L, 10L, ReactionType.LIKE))
                .thenReturn(new PostReactionResult(10L, true, false, 6, 2));

        mockMvc.perform(post("/api/v1/posts/10/likes").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.postId").value(10))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.isDisliked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(6))
                .andExpect(jsonPath("$.data.dislikeCount").value(2));
    }

    @Test
    void 게시글_싫어요_토글은_200과_반응_상태를_반환한다() throws Exception {
        when(togglePostReactionUseCase.toggle(1L, 10L, ReactionType.DISLIKE))
                .thenReturn(new PostReactionResult(10L, false, true, 5, 3));

        mockMvc.perform(post("/api/v1/posts/10/dislikes").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDisliked").value(true))
                .andExpect(jsonPath("$.data.dislikeCount").value(3));
    }

    @Test
    void 없는_게시글이면_404를_반환한다() throws Exception {
        when(togglePostReactionUseCase.toggle(1L, 999L, ReactionType.LIKE)).thenThrow(
                new PostApplicationException(PostErrorCode.POST_NOT_FOUND, "존재하지 않거나 삭제된 게시글입니다."));

        mockMvc.perform(post("/api/v1/posts/999/likes").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("POST_NOT_FOUND"));
    }

    @Test
    void 인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/posts/10/likes"))
                .andExpect(status().isUnauthorized());
    }
}
