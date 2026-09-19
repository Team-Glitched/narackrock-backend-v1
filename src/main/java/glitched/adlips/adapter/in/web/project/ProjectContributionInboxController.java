package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.response.ProjectContributionInboxResponse;
import glitched.adlips.application.project.usecase.ProjectContributionInboxUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "기여 알림", description = "원곡자에게 도착한 승인 대기 기여 요청")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/notifications/contributions")
public class ProjectContributionInboxController {
    private final ProjectContributionInboxUseCase inbox;
    private final AuthenticatedUserResolver users;

    public ProjectContributionInboxController(ProjectContributionInboxUseCase inbox,
                                               AuthenticatedUserResolver users) {
        this.inbox = inbox;
        this.users = users;
    }

    @GetMapping
    public ApiResponse<ProjectContributionInboxResponse> get(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.success("기여 요청 알림 조회가 완료되었습니다.",
                inbox.execute(users.requireUserId(authorization)));
    }
}
