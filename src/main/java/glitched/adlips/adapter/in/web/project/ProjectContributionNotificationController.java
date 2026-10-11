package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.response.ProjectContributionNotificationResponse;
import glitched.adlips.application.project.usecase.ProjectContributionNotificationUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림", description = "사용자 알림 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/notifications/contributions")
public class ProjectContributionNotificationController {
    private final ProjectContributionNotificationUseCase useCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ProjectContributionNotificationController(
            ProjectContributionNotificationUseCase useCase,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.useCase = useCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ApiResponse<ProjectContributionNotificationResponse> getNotifications(
            @RequestHeader("Authorization") String authorization) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "작곡 참여 요청 알림 조회가 완료되었습니다.",
                useCase.execute(userId));
    }
}
