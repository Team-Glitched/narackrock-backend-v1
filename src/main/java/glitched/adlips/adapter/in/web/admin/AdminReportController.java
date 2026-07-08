package glitched.adlips.adapter.in.web.admin;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ReportRejectRequest;
import glitched.adlips.adapter.in.web.dto.ReportRejectResponse;
import glitched.adlips.adapter.in.web.dto.ReportResolveRequest;
import glitched.adlips.adapter.in.web.dto.ReportResolveResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.report.usecase.ReportResolutionResult;
import glitched.adlips.application.report.usecase.RejectReportUseCase;
import glitched.adlips.application.report.usecase.ResolveReportUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {

    private final ResolveReportUseCase resolveReportUseCase;
    private final RejectReportUseCase rejectReportUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminReportController(
            ResolveReportUseCase resolveReportUseCase,
            RejectReportUseCase rejectReportUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.resolveReportUseCase = resolveReportUseCase;
        this.rejectReportUseCase = rejectReportUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PatchMapping("/{reportId}/resolve")
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

    @PatchMapping("/{reportId}/reject")
    public ResponseEntity<ApiResponse<ReportRejectResponse>> reject(
            @PathVariable Long reportId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody ReportRejectRequest request
    ) {
        Long adminUserId = authenticatedUserResolver.requireUserId(authorization);
        ReportResolutionResult result = rejectReportUseCase.execute(reportId, adminUserId, request.reason());
        return ResponseEntity.ok(ApiResponse.success(
                "신고가 반려 처리되었습니다.", ReportRejectResponse.from(result)));
    }
}
