package glitched.adlips.adapter.in.web.community;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.PostReportRequest;
import glitched.adlips.adapter.in.web.dto.PostReportResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.report.usecase.ReportSubmissionResult;
import glitched.adlips.application.report.usecase.SubmitReportUseCase;
import glitched.adlips.domain.report.ReportTargetType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "게시글 신고", description = "게시글 및 게시글 댓글 신고 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/posts")
public class PostReportController {

    private final SubmitReportUseCase submitReportUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public PostReportController(
            SubmitReportUseCase submitReportUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.submitReportUseCase = submitReportUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{postId}/reports")
    public ResponseEntity<ApiResponse<PostReportResponse>> reportPost(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody PostReportRequest request
    ) {
        return submit(
                ReportTargetType.POST,
                postId,
                authorization,
                request,
                "게시글 신고가 성공적으로 접수되었습니다."
        );
    }

    @PostMapping("/comments/{commentId}/reports")
    public ResponseEntity<ApiResponse<PostReportResponse>> reportComment(
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody PostReportRequest request
    ) {
        return submit(
                ReportTargetType.POST_COMMENT,
                commentId,
                authorization,
                request,
                "댓글 신고가 성공적으로 접수되었습니다."
        );
    }

    private ResponseEntity<ApiResponse<PostReportResponse>> submit(
            ReportTargetType targetType,
            Long targetId,
            String authorization,
            PostReportRequest request,
            String message
    ) {
        Long reporterId = authenticatedUserResolver.requireUserId(authorization);
        ReportSubmissionResult result = submitReportUseCase.execute(
                targetType,
                targetId,
                reporterId,
                request.reason(),
                request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, PostReportResponse.from(result)));
    }
}
