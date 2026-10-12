package glitched.adlips.adapter.in.web.admin;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.adapter.in.web.dto.GalleryProposalPageResponse;
import glitched.adlips.adapter.in.web.dto.GalleryProposalRejectRequest;
import glitched.adlips.adapter.in.web.dto.GalleryProposalReviewResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.ApproveGalleryProposalUseCase;
import glitched.adlips.application.community.GetGalleryProposalsUseCase;
import glitched.adlips.application.community.RejectGalleryProposalUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 갤러리 개설", description = "갤러리 개설 요청 승인·반려 관리자 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/gallery-proposals")
public class AdminGalleryProposalController {

    private final GetGalleryProposalsUseCase getGalleryProposalsUseCase;
    private final ApproveGalleryProposalUseCase approveGalleryProposalUseCase;
    private final RejectGalleryProposalUseCase rejectGalleryProposalUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminGalleryProposalController(
            GetGalleryProposalsUseCase getGalleryProposalsUseCase,
            ApproveGalleryProposalUseCase approveGalleryProposalUseCase,
            RejectGalleryProposalUseCase rejectGalleryProposalUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getGalleryProposalsUseCase = getGalleryProposalsUseCase;
        this.approveGalleryProposalUseCase = approveGalleryProposalUseCase;
        this.rejectGalleryProposalUseCase = rejectGalleryProposalUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<GalleryProposalPageResponse>> getProposals(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long adminUserId = authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "갤러리 개설 요청 목록 조회가 완료되었습니다.",
                GalleryProposalPageResponse.from(getGalleryProposalsUseCase.execute(adminUserId, status, cursor, size))));
    }

    @PatchMapping("/{proposalId}/approve")
    public ResponseEntity<ApiResponse<GalleryProposalReviewResponse>> approve(
            @PathVariable Long proposalId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long adminUserId = authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "갤러리 개설 요청이 승인되었습니다.",
                GalleryProposalReviewResponse.from(approveGalleryProposalUseCase.execute(proposalId, adminUserId))));
    }

    @PatchMapping("/{proposalId}/reject")
    public ResponseEntity<ApiResponse<GalleryProposalReviewResponse>> reject(
            @PathVariable Long proposalId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody GalleryProposalRejectRequest request
    ) {
        Long adminUserId = authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "갤러리 개설 요청이 반려되었습니다.",
                GalleryProposalReviewResponse.from(
                        rejectGalleryProposalUseCase.execute(proposalId, adminUserId, request.reason()))));
    }
}
