package glitched.adlips.adapter.in.web.shorts;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortReportRequest;
import glitched.adlips.adapter.in.web.dto.ShortReportResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.ShortReportResult;
import glitched.adlips.application.shorts.SubmitShortReportUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "숏폼 신고", description = "숏폼 신고 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/shorts")
public class ShortReportController {

    private final SubmitShortReportUseCase submitShortReportUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortReportController(
            SubmitShortReportUseCase submitShortReportUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.submitShortReportUseCase = submitShortReportUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{shortId}/reports")
    public ResponseEntity<ApiResponse<ShortReportResponse>> report(
            @PathVariable Long shortId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody ShortReportRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        ShortReportResult result = submitShortReportUseCase.execute(
                shortId, userId, request.reason(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("숏폼 신고가 접수되었습니다.", ShortReportResponse.from(result)));
    }
}
