package glitched.adlips.adapter.in.web.community;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.adapter.in.web.dto.GalleryProposalCreateRequest;
import glitched.adlips.adapter.in.web.dto.GalleryProposalCreateResponse;
import glitched.adlips.adapter.in.web.dto.GalleryProposalResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.GetMyGalleryProposalsUseCase;
import glitched.adlips.application.community.SubmitGalleryProposalUseCase;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "갤러리 개설 요청", description = "갤러리 개설 요청 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping({"/api/v1/community-proposals", "/api/v1/gallery-proposals"})
public class GalleryProposalController {

    private final SubmitGalleryProposalUseCase submitGalleryProposalUseCase;
    private final GetMyGalleryProposalsUseCase getMyGalleryProposalsUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public GalleryProposalController(
            SubmitGalleryProposalUseCase submitGalleryProposalUseCase,
            GetMyGalleryProposalsUseCase getMyGalleryProposalsUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.submitGalleryProposalUseCase = submitGalleryProposalUseCase;
        this.getMyGalleryProposalsUseCase = getMyGalleryProposalsUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GalleryProposalCreateResponse>> submit(
            @RequestHeader("Authorization") String authorization,
            @RequestBody GalleryProposalCreateRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        Long proposalId = submitGalleryProposalUseCase.execute(userId, request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "갤러리 개설 요청이 접수되었습니다.", GalleryProposalCreateResponse.pending(proposalId)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<GalleryProposalResponse>>> getMine(
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        List<GalleryProposalResponse> proposals = getMyGalleryProposalsUseCase.execute(userId).stream()
                .map(GalleryProposalResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("내 갤러리 개설 요청 목록 조회가 완료되었습니다.", proposals));
    }
}
