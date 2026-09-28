package glitched.adlips.adapter.in.web.community;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.PostShareResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.GetPostShareUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 숏폼 공유(ShortsShareController)와 같은 계약이다.
@RestController
@Tag(name = "게시글 공유", description = "게시글 공유 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/posts")
public class PostShareController {

    private final GetPostShareUseCase getPostShareUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public PostShareController(
            GetPostShareUseCase getPostShareUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getPostShareUseCase = getPostShareUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{postId}/share")
    public ResponseEntity<ApiResponse<PostShareResponse>> getShareLink(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authorization
    ) {
        authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "공유 링크 조회가 완료되었습니다.", PostShareResponse.from(getPostShareUseCase.execute(postId))));
    }
}
