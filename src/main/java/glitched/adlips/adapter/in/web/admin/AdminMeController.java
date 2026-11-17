package glitched.adlips.adapter.in.web.admin;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.adapter.in.web.dto.OperatorAdminStatusResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.user.admin.usecase.GetOperatorAdminStatusUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영자 어드민", description = "운영자 어드민 여부 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/me")
public class AdminMeController {

    private final GetOperatorAdminStatusUseCase getOperatorAdminStatusUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminMeController(
            GetOperatorAdminStatusUseCase getOperatorAdminStatusUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getOperatorAdminStatusUseCase = getOperatorAdminStatusUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OperatorAdminStatusResponse>> getStatus(
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "운영자 어드민 여부 조회가 완료되었습니다.",
                new OperatorAdminStatusResponse(getOperatorAdminStatusUseCase.execute(userId))));
    }
}
