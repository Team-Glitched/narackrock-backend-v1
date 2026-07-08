package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortCommentRequest;
import glitched.adlips.adapter.in.web.dto.ShortCommentResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.ShortCommentResult;
import glitched.adlips.application.shorts.SubmitShortCommentUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shorts")
public class ShortCommentController {

    private final SubmitShortCommentUseCase submitShortCommentUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortCommentController(
            SubmitShortCommentUseCase submitShortCommentUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.submitShortCommentUseCase = submitShortCommentUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{shortId}/comments")
    public ResponseEntity<ApiResponse<ShortCommentResponse>> create(
            @PathVariable Long shortId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody ShortCommentRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        ShortCommentResult result = submitShortCommentUseCase.execute(
                shortId, userId, request.content(), request.parentCommentId());
        String message = result.parentCommentId() == null
                ? "댓글이 성공적으로 등록되었습니다." : "대댓글이 성공적으로 등록되었습니다.";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, ShortCommentResponse.from(result)));
    }
}
