package glitched.adlips.adapter.in.web.admin;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ReportResolveRequest;
import glitched.adlips.adapter.in.web.dto.ReportResolveResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.report.usecase.ReportResolutionResult;
import glitched.adlips.application.report.usecase.ResolveReportUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {

    private final ResolveReportUseCase resolveReportUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminReportController(
            ResolveReportUseCase resolveReportUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.resolveReportUseCase = resolveReportUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{reportId}/resolve")
    public ResponseEntity<ApiResponse<ReportResolveResponse>> resolve(
            @PathVariable Long reportId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody ReportResolveRequest request
    ) {
        Long adminUserId = authenticatedUserResolver.requireUserId(authorization);
        ReportResolutionResult result = resolveReportUseCase.execute(
                reportId, adminUserId, request.actionType(), request.reason());
        return ResponseEntity.ok(ApiResponse.success(
                "해당 신고 내역이 성공적으로 처리되었습니다.", ReportResolveResponse.from(result)));
    }
}
