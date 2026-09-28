package glitched.adlips.adapter.in.web.community;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.PostReactionResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.TogglePostReactionUseCase;
import glitched.adlips.domain.reaction.ReactionType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "게시글 반응", description = "게시글 좋아요 및 싫어요 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/posts")
public class PostReactionController {

    private final TogglePostReactionUseCase togglePostReactionUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public PostReactionController(
            TogglePostReactionUseCase togglePostReactionUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.togglePostReactionUseCase = togglePostReactionUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<PostReactionResponse>> toggleLike(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authorization
    ) {
        return toggle(postId, authorization, ReactionType.LIKE, "게시글 좋아요 상태가 변경되었습니다.");
    }

    @PostMapping("/{postId}/dislikes")
    public ResponseEntity<ApiResponse<PostReactionResponse>> toggleDislike(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authorization
    ) {
        return toggle(postId, authorization, ReactionType.DISLIKE, "게시글 싫어요 상태가 변경되었습니다.");
    }

    private ResponseEntity<ApiResponse<PostReactionResponse>> toggle(
            Long postId, String authorization, ReactionType reactionType, String message) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                message, PostReactionResponse.from(togglePostReactionUseCase.toggle(userId, postId, reactionType))));
    }
}
