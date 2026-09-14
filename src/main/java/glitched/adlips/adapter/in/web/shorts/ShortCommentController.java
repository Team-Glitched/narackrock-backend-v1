package glitched.adlips.adapter.in.web.shorts;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortCommentLikeResponse;
import glitched.adlips.adapter.in.web.dto.ShortCommentListResponse;
import glitched.adlips.adapter.in.web.dto.ShortCommentReportRequest;
import glitched.adlips.adapter.in.web.dto.ShortCommentReportResponse;
import glitched.adlips.adapter.in.web.dto.ShortCommentRequest;
import glitched.adlips.adapter.in.web.dto.ShortCommentResponse;
import glitched.adlips.adapter.in.web.dto.ShortCommentUpdateRequest;
import glitched.adlips.adapter.in.web.dto.ShortCommentUpdateResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.DeleteShortCommentUseCase;
import glitched.adlips.application.shorts.GetShortCommentsUseCase;
import glitched.adlips.application.shorts.ShortCommentLikeResult;
import glitched.adlips.application.shorts.ShortCommentListResult;
import glitched.adlips.application.shorts.ShortCommentReportResult;
import glitched.adlips.application.shorts.ShortCommentResult;
import glitched.adlips.application.shorts.SubmitShortCommentReportUseCase;
import glitched.adlips.application.shorts.SubmitShortCommentUseCase;
import glitched.adlips.application.shorts.ToggleShortCommentLikeUseCase;
import glitched.adlips.application.shorts.UpdateShortCommentUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "숏폼 댓글", description = "숏폼 댓글 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/shorts")
public class ShortCommentController {

    private final SubmitShortCommentUseCase submitShortCommentUseCase;
    private final GetShortCommentsUseCase getShortCommentsUseCase;
    private final UpdateShortCommentUseCase updateShortCommentUseCase;
    private final DeleteShortCommentUseCase deleteShortCommentUseCase;
    private final ToggleShortCommentLikeUseCase toggleShortCommentLikeUseCase;
    private final SubmitShortCommentReportUseCase submitShortCommentReportUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortCommentController(
            SubmitShortCommentUseCase submitShortCommentUseCase,
            GetShortCommentsUseCase getShortCommentsUseCase,
            UpdateShortCommentUseCase updateShortCommentUseCase,
            DeleteShortCommentUseCase deleteShortCommentUseCase,
            ToggleShortCommentLikeUseCase toggleShortCommentLikeUseCase,
            SubmitShortCommentReportUseCase submitShortCommentReportUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.submitShortCommentUseCase = submitShortCommentUseCase;
        this.getShortCommentsUseCase = getShortCommentsUseCase;
        this.updateShortCommentUseCase = updateShortCommentUseCase;
        this.deleteShortCommentUseCase = deleteShortCommentUseCase;
        this.toggleShortCommentLikeUseCase = toggleShortCommentLikeUseCase;
        this.submitShortCommentReportUseCase = submitShortCommentReportUseCase;
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

    @GetMapping("/{shortId}/comments")
    public ResponseEntity<ApiResponse<ShortCommentListResponse>> getComments(
            @PathVariable Long shortId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long viewerId = authenticatedUserResolver.resolveOptionalUserId(authorization);
        ShortCommentListResult result = getShortCommentsUseCase.execute(shortId, viewerId);
        return ResponseEntity.ok(ApiResponse.success(
                "댓글 목록 조회가 완료되었습니다.", ShortCommentListResponse.from(result)));
    }

    @PutMapping("/{shortId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<ShortCommentUpdateResponse>> update(
            @PathVariable Long shortId,
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody ShortCommentUpdateRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        Long updatedCommentId = updateShortCommentUseCase.execute(shortId, commentId, userId, request.content());
        return ResponseEntity.ok(ApiResponse.success(
                "댓글이 성공적으로 수정되었습니다.", new ShortCommentUpdateResponse(updatedCommentId)));
    }

    @DeleteMapping("/{shortId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long shortId,
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        deleteShortCommentUseCase.execute(shortId, commentId, userId);
        return ResponseEntity.ok(ApiResponse.success("댓글이 성공적으로 삭제되었습니다."));
    }

    @PostMapping("/{shortId}/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<ShortCommentLikeResponse>> toggleLike(
            @PathVariable Long shortId,
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        ShortCommentLikeResult result = toggleShortCommentLikeUseCase.toggle(userId, shortId, commentId);
        return ResponseEntity.ok(ApiResponse.success(
                "댓글 좋아요 상태가 성공적으로 반영되었습니다.", ShortCommentLikeResponse.from(result)));
    }

    @PostMapping("/{shortId}/comments/{commentId}/reports")
    public ResponseEntity<ApiResponse<ShortCommentReportResponse>> report(
            @PathVariable Long shortId,
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody ShortCommentReportRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        ShortCommentReportResult result = submitShortCommentReportUseCase.execute(
                shortId, commentId, userId, request.reason(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "댓글 신고가 성공적으로 접수되었습니다.", ShortCommentReportResponse.from(result)));
    }
}
