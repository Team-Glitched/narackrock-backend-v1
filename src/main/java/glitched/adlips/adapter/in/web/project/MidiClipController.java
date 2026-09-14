package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.MidiClipSaveRequest;
import glitched.adlips.application.project.dto.response.MidiClipSaveResponse;
import glitched.adlips.application.project.usecase.MidiClipSaveUseCase;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clips/{clipId}/midi")
public class MidiClipController {
    private final MidiClipSaveUseCase saveUseCase;
    private final AuthenticatedUserResolver userResolver;

    public MidiClipController(MidiClipSaveUseCase saveUseCase, AuthenticatedUserResolver userResolver) {
        this.saveUseCase = saveUseCase;
        this.userResolver = userResolver;
    }

    @PatchMapping
    public ApiResponse<MidiClipSaveResponse> save(
            @PathVariable Long clipId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody MidiClipSaveRequest request) {
        var response = saveUseCase.execute(new MidiClipSaveRequest(
                clipId, userResolver.requireUserId(authorization),
                request.startTick(), request.durationTick(), request.midiNotes()));
        return ApiResponse.success("MIDI 클립이 저장되었습니다.", response);
    }
}
