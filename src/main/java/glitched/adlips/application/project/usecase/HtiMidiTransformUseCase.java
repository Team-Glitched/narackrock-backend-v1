package glitched.adlips.application.project.usecase;

import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.HtiMidiTransformRequest;
import glitched.adlips.application.project.dto.request.MidiNoteRequest;
import glitched.adlips.application.project.dto.response.HtiMidiTransformResponse;
import glitched.adlips.application.project.port.out.HtiMidiTransformPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.domain.project.ProjectMemberRole;

public class HtiMidiTransformUseCase {
    private final ProjectRepositoryPort projects;
    private final ProjectMemberRepositoryPort members;
    private final HtiMidiTransformPort transformer;

    public HtiMidiTransformUseCase(ProjectRepositoryPort projects,
                                   ProjectMemberRepositoryPort members,
                                   HtiMidiTransformPort transformer) {
        this.projects = projects;
        this.members = members;
        this.transformer = transformer;
    }

    public HtiMidiTransformResponse execute(HtiMidiTransformRequest request) {
        validateAudio(request);
        var project = projects.findByIdAndDeletedAtIsNull(request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND,
                        "존재하지 않거나 삭제된 프로젝트입니다."));
        members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .filter(member -> member.getRole() != ProjectMemberRole.VIEWER)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED,
                        "해당 프로젝트를 편집할 권한이 없습니다."));

        HtiMidiTransformPort.Result result;
        try {
            result = transformer.transform(new HtiMidiTransformPort.Command(
                    request.projectId(), request.filename(), request.contentType(), request.content(),
                    project.getBpm(), project.getPpq(), request.soundKind() == null
                            ? glitched.adlips.domain.project.AudioTranscriptionMode.AUTO : request.soundKind()));
        } catch (HtiMidiTransformPort.InvalidAudioException exception) {
            throw error(ProjectErrorCode.INVALID_HTI_AUDIO, "녹음에서 분석할 소리를 찾지 못했거나 파일을 읽을 수 없습니다. 음량과 파일을 확인해 주세요.");
        } catch (HtiMidiTransformPort.HtiServiceException exception) {
            throw error(ProjectErrorCode.HTI_TRANSFORM_FAILED,
                    "오디오 변환 서버에서 녹음을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        if (result == null || result.durationTick() <= 0 || result.midiNotes() == null) {
            throw error(ProjectErrorCode.HTI_TRANSFORM_FAILED, "HTI 변환 결과가 올바르지 않습니다.");
        }
        if (result.durationTick() > project.getMaxTick()) {
            throw error(ProjectErrorCode.PROJECT_DURATION_LIMIT_EXCEEDED,
                    "녹음은 곡의 최대 길이인 1분을 초과할 수 없습니다.");
        }
        var notes = result.midiNotes().stream()
                .map(note -> new MidiNoteRequest(
                        note.pitch(), note.startTick(), note.durationTick(), note.velocity(),
                        note.effectType(), note.effectParams()))
                .toList();
        return new HtiMidiTransformResponse(result.durationTick(), notes, result.routing());
    }

    private void validateAudio(HtiMidiTransformRequest request) {
        if (request == null || request.projectId() == null || request.userId() == null
                || request.filename() == null || request.filename().isBlank()
                || request.content() == null || request.content().length == 0) {
            throw error(ProjectErrorCode.INVALID_HTI_AUDIO, "변환할 녹음 파일이 올바르지 않습니다.");
        }
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
