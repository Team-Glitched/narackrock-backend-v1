package glitched.adlips.adapter.in.web.community;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.PostCommentReactionResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.PostCommentReactionResult;
import glitched.adlips.application.community.TogglePostCommentReactionUseCase;
import glitched.adlips.domain.reaction.ReactionType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "게시글 댓글 반응", description = "게시글 댓글 좋아요 및 싫어요 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/posts")
public class PostCommentReactionController {

    private final TogglePostCommentReactionUseCase togglePostCommentReactionUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public PostCommentReactionController(
            TogglePostCommentReactionUseCase togglePostCommentReactionUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.togglePostCommentReactionUseCase = togglePostCommentReactionUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<PostCommentReactionResponse>> toggleLike(
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization
    ) {
        return toggle(commentId, authorization, ReactionType.LIKE, "댓글 좋아요 상태가 변경되었습니다.");
    }

    @PostMapping("/comments/{commentId}/dislike")
    public ResponseEntity<ApiResponse<PostCommentReactionResponse>> toggleDislike(
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization
    ) {
        return toggle(commentId, authorization, ReactionType.DISLIKE, "댓글 싫어요 상태가 변경되었습니다.");
    }

    private ResponseEntity<ApiResponse<PostCommentReactionResponse>> toggle(
            Long commentId, String authorization, ReactionType reactionType, String message) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        PostCommentReactionResult result = togglePostCommentReactionUseCase.toggle(userId, commentId, reactionType);
        return ResponseEntity.ok(ApiResponse.success(message, PostCommentReactionResponse.from(result)));
    }
}
