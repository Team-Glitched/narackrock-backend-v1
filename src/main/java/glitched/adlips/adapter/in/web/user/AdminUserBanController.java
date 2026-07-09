package glitched.adlips.adapter.in.web.user;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.application.user.admin.dto.request.UserBanCancelRequest;
import glitched.adlips.application.user.admin.dto.request.UserBanCreateRequest;
import glitched.adlips.application.user.admin.dto.response.UserBanCancelResponse;
import glitched.adlips.application.user.admin.dto.response.UserBanCreateResponse;
import glitched.adlips.application.user.admin.usecase.UserBanCancelUseCase;
import glitched.adlips.application.user.admin.usecase.UserBanCreateUseCase;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users/{userId}/bans")
public class AdminUserBanController {
    private final UserBanCreateUseCase userBanCreateUseCase;
    private final UserBanCancelUseCase userBanCancelUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminUserBanController(
            UserBanCreateUseCase userBanCreateUseCase,
            UserBanCancelUseCase userBanCancelUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.userBanCreateUseCase = userBanCreateUseCase;
        this.userBanCancelUseCase = userBanCancelUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping
    public ApiResponse<UserBanCreateResponse> ban(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody UserBanCreateRequest request
    ) {
        Long adminId = authenticatedUserResolver.requireUserId(authorization);
        UserBanCreateResponse response = userBanCreateUseCase.execute(new UserBanCreateRequest(
                adminId,
                userId,
                request.banDurationDays(),
                request.banReason()
        ));
        return ApiResponse.success("해당 유저가 성공적으로 차단되었습니다.", response);
    }

    @DeleteMapping
    public ApiResponse<UserBanCancelResponse> cancel(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long adminId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "해당 유저의 차단이 해제되었습니다.",
                userBanCancelUseCase.execute(new UserBanCancelRequest(adminId, userId))
        );
    }
}
