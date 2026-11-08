package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.SentContributionGetListRequest;
import glitched.adlips.application.project.dto.response.SentContributionGetListResponse;
import glitched.adlips.application.project.usecase.SentContributionGetListUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로젝트 기여", description = "보낸 참여 요청 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users/me/contributions")
public class SentContributionGetListController {
    private final SentContributionGetListUseCase useCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public SentContributionGetListController(SentContributionGetListUseCase useCase,
                                             AuthenticatedUserResolver authenticatedUserResolver) {
        this.useCase = useCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ApiResponse<SentContributionGetListResponse> getList(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success("보낸 참여 요청 조회가 완료되었습니다.",
                useCase.execute(new SentContributionGetListRequest(userId, cursor, size)));
    }
}
