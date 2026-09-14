package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.AudioClipCreateRequest;
import glitched.adlips.application.project.dto.response.AudioClipCreateResponse;
import glitched.adlips.application.project.dto.request.MidiClipCreateRequest;
import glitched.adlips.application.project.dto.response.MidiClipCreateResponse;
import glitched.adlips.application.project.usecase.AudioClipCreateUseCase;
import glitched.adlips.application.project.usecase.MidiClipCreateUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tracks/{trackId}/clips")
public class ClipController {
    private final AudioClipCreateUseCase audioClipCreateUseCase;
    private final MidiClipCreateUseCase midiClipCreateUseCase;
    private final AuthenticatedUserResolver userResolver;

    public ClipController(AudioClipCreateUseCase audioClipCreateUseCase,
                          MidiClipCreateUseCase midiClipCreateUseCase,
                          AuthenticatedUserResolver userResolver) {
        this.audioClipCreateUseCase = audioClipCreateUseCase;
        this.midiClipCreateUseCase = midiClipCreateUseCase;
        this.userResolver = userResolver;
    }

    @PostMapping("/audio")
    public ResponseEntity<ApiResponse<AudioClipCreateResponse>> createAudio(
            @PathVariable Long projectId,
            @PathVariable Long trackId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody AudioClipCreateRequest request) {
        var response = audioClipCreateUseCase.execute(new AudioClipCreateRequest(
                projectId, trackId, userResolver.requireUserId(authorization), request.mediaFileId(),
                request.sourceType(), request.startTick(), request.durationTick(), request.clipOffsetMs()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("오디오 클립이 생성되었습니다.", response));
    }

    @PostMapping("/midi")
    public ResponseEntity<ApiResponse<MidiClipCreateResponse>> createMidi(
            @PathVariable Long projectId,
            @PathVariable Long trackId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody MidiClipCreateRequest request) {
        var response = midiClipCreateUseCase.execute(new MidiClipCreateRequest(
                projectId, trackId, userResolver.requireUserId(authorization),
                request.startTick(), request.durationTick(), request.midiNotes()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("MIDI 클립이 생성되었습니다.", response));
    }
}
