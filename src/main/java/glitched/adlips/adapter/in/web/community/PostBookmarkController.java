package glitched.adlips.adapter.in.web.community;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.PostBookmarkResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.PostBookmarkResult;
import glitched.adlips.application.community.TogglePostBookmarkUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "게시글 북마크", description = "게시글 북마크 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/posts")
public class PostBookmarkController {

    private final TogglePostBookmarkUseCase togglePostBookmarkUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public PostBookmarkController(
            TogglePostBookmarkUseCase togglePostBookmarkUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.togglePostBookmarkUseCase = togglePostBookmarkUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{postId}/bookmark")
    public ResponseEntity<ApiResponse<PostBookmarkResponse>> toggleBookmark(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        PostBookmarkResult result = togglePostBookmarkUseCase.toggle(userId, postId);
        return ResponseEntity.ok(ApiResponse.success(
                "게시글 북마크 상태가 변경되었습니다.", PostBookmarkResponse.from(result)));
    }
}
