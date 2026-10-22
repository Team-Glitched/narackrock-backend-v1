package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.GetShortsParticipantTracksUseCase;
import glitched.adlips.application.shorts.ShortsParticipantTracksResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shorts")
@Tag(name = "숏폼 작곡가")
@SecurityRequirement(name = "bearerAuth")
public class ShortsParticipantTracksController {
    private final GetShortsParticipantTracksUseCase useCase;
    private final AuthenticatedUserResolver users;

    public ShortsParticipantTracksController(GetShortsParticipantTracksUseCase useCase, AuthenticatedUserResolver users) {
        this.useCase = useCase; this.users = users;
    }

    @GetMapping("/{shortId}/participant-tracks")
    @Operation(summary = "참여자 선택 재생용 승인 트랙 조회", description = "작곡 프로젝트에 가입하지 않고 공개 숏폼의 승인된 트랙·클립만 조회합니다.")
    public ApiResponse<ShortsParticipantTracksResult> get(@PathVariable Long shortId,
            @RequestHeader("Authorization") String authorization) {
        users.requireUserId(authorization);
        return ApiResponse.success("참여자 트랙 조회가 완료되었습니다.", useCase.execute(shortId));
    }
}
