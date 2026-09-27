package glitched.adlips.adapter.in.web.community;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.community.PostCommentApplicationException;
import glitched.adlips.application.community.PostCommentErrorCode;
import glitched.adlips.application.community.PostCommentReactionResult;
import glitched.adlips.application.community.TogglePostCommentReactionUseCase;
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

@WebMvcTest(PostCommentReactionController.class)
@Import(SecurityConfig.class)
class PostCommentReactionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean TogglePostCommentReactionUseCase togglePostCommentReactionUseCase;
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
    void 댓글_좋아요_토글은_200과_반응_상태를_반환한다() throws Exception {
        when(togglePostCommentReactionUseCase.toggle(1L, 30L, ReactionType.LIKE))
                .thenReturn(new PostCommentReactionResult(30L, true, false, 3, 0));

        mockMvc.perform(post("/api/v1/posts/comments/30/like").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value(30))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.isDisliked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(3))
                .andExpect(jsonPath("$.data.dislikeCount").value(0));
    }

    @Test
    void 댓글_싫어요_토글은_200과_반응_상태를_반환한다() throws Exception {
        when(togglePostCommentReactionUseCase.toggle(1L, 30L, ReactionType.DISLIKE))
                .thenReturn(new PostCommentReactionResult(30L, false, true, 2, 1));

        mockMvc.perform(post("/api/v1/posts/comments/30/dislike").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDisliked").value(true))
                .andExpect(jsonPath("$.data.dislikeCount").value(1));
    }

    @Test
    void 없는_댓글이면_404를_반환한다() throws Exception {
        when(togglePostCommentReactionUseCase.toggle(1L, 999L, ReactionType.LIKE)).thenThrow(
                new PostCommentApplicationException(PostCommentErrorCode.COMMENT_NOT_FOUND, "존재하지 않거나 삭제된 댓글입니다."));

        mockMvc.perform(post("/api/v1/posts/comments/999/like").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void 인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/posts/comments/30/like"))
                .andExpect(status().isUnauthorized());
    }
}
