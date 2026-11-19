package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.HtiMidiTransformRequest;
import glitched.adlips.application.project.dto.response.HtiMidiTransformResponse;
import glitched.adlips.application.project.usecase.HtiMidiTransformUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import glitched.adlips.domain.project.AudioTranscriptionMode;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "HTI MIDI 변환", description = "녹음을 MIDI 노트로 변환하는 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/hti-transform")
public class HtiMidiTransformController {
    private final HtiMidiTransformUseCase useCase;
    private final AuthenticatedUserResolver userResolver;

    public HtiMidiTransformController(HtiMidiTransformUseCase useCase,
                                      AuthenticatedUserResolver userResolver) {
        this.useCase = useCase;
        this.userResolver = userResolver;
    }

    public ApiResponse<HtiMidiTransformResponse> transform(Long projectId, String authorization, MultipartFile audio) {
        return transform(projectId, authorization, audio, AudioTranscriptionMode.AUTO);
    }

    @PostMapping
    public ApiResponse<HtiMidiTransformResponse> transform(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authorization,
            @RequestPart("audio") MultipartFile audio,
            @RequestParam(defaultValue = "AUTO") AudioTranscriptionMode soundKind) {
        try {
            var response = useCase.execute(new HtiMidiTransformRequest(
                    projectId,
                    userResolver.requireUserId(authorization),
                    audio.getOriginalFilename(),
                    audio.getContentType(),
                    audio.getBytes(), soundKind));
            return ApiResponse.success("녹음을 MIDI로 변환했습니다.", response);
        } catch (IOException exception) {
            throw new ProjectApplicationException(
                    ProjectErrorCode.INVALID_HTI_AUDIO, "녹음 파일을 읽지 못했습니다.");
        }
    }
}
