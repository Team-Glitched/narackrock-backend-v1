package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.usecase.AnalyzeAudioClipUseCase;
import glitched.adlips.domain.project.AudioAnalysis;
import glitched.adlips.domain.project.AudioSoundKind;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AudioClipAnalysisController {
    private final AnalyzeAudioClipUseCase analysis;
    private final AuthenticatedUserResolver users;

    public AudioClipAnalysisController(AnalyzeAudioClipUseCase analysis, AuthenticatedUserResolver users) {
        this.analysis = analysis;
        this.users = users;
    }

    @PostMapping("/api/v1/projects/{projectId}/clips/{clipId}/audio-analysis")
    public ApiResponse<AudioAnalysis> analyze(@PathVariable Long projectId, @PathVariable Long clipId,
            @RequestHeader("Authorization") String authorization, @RequestBody AnalyzeRequest request) {
        return ApiResponse.success("오디오 분석을 저장했습니다.", analysis.execute(projectId, clipId,
                users.requireUserId(authorization), request.soundKind(), Boolean.TRUE.equals(request.refresh())));
    }

    public record AnalyzeRequest(AudioSoundKind soundKind, Boolean refresh) {}
}
