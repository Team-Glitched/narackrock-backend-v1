package glitched.adlips.application.project.usecase;

import glitched.adlips.adapter.out.persistence.project.ProjectClipJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectTrackJpaRepository;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.MidiClipCreateRequest;
import glitched.adlips.application.project.dto.response.MidiClipCreateResponse;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectMemberRole;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MidiClipCreateUseCase {
    private final ProjectJpaRepository projects;
    private final ProjectMemberJpaRepository members;
    private final ProjectTrackJpaRepository tracks;
    private final ProjectClipJpaRepository clips;
    private final UserRepositoryPort users;

    public MidiClipCreateUseCase(ProjectJpaRepository projects, ProjectMemberJpaRepository members,
                                 ProjectTrackJpaRepository tracks, ProjectClipJpaRepository clips,
                                 UserRepositoryPort users) {
        this.projects = projects;
        this.members = members;
        this.tracks = tracks;
        this.clips = clips;
        this.users = users;
    }

    @Transactional
    public MidiClipCreateResponse execute(MidiClipCreateRequest request) {
        if (request == null || request.projectId() == null || request.trackId() == null
                || request.userId() == null || request.midiNotes() == null) {
            throw error(ProjectErrorCode.INVALID_MIDI_NOTE_DATA, "MIDI 노트 데이터가 올바르지 않습니다.");
        }
        var project = projects.findByIdAndDeletedAtIsNull(request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않거나 삭제된 프로젝트입니다."));
        members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .filter(member -> member.getRole() != ProjectMemberRole.VIEWER)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "해당 프로젝트에 접근할 권한이 없습니다."));
        var track = tracks.findByIdAndIsDeletedFalse(request.trackId())
                .orElseThrow(() -> error(ProjectErrorCode.TRACK_NOT_FOUND, "존재하지 않거나 삭제된 트랙입니다."));
        if (!track.belongsTo(project)) {
            throw error(ProjectErrorCode.TRACK_NOT_IN_PROJECT, "해당 프로젝트에 속한 트랙이 아닙니다.");
        }
        if (track.getInstrument() == null || track.getInstrument().isBlank()) {
            throw error(ProjectErrorCode.TRACK_INSTRUMENT_NOT_SET,
                    "MIDI 클립을 생성하려면 트랙의 악기가 먼저 설정되어야 합니다.");
        }
        validateRange(request.startTick(), request.durationTick(), project.getMaxTick());
        var user = users.findById(request.userId()).filter(it -> it.isActive())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "해당 프로젝트에 접근할 권한이 없습니다."));
        int sortOrder = clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(request.trackId()).size();
        ProjectClip clip;
        try {
            clip = clips.save(ProjectClip.createMidi(
                    project, track, user, request.startTick(), request.durationTick(),
                    List.copyOf(request.midiNotes()), sortOrder));
        } catch (IllegalArgumentException exception) {
            throw error(ProjectErrorCode.INVALID_MIDI_NOTE_DATA, "MIDI 노트 데이터가 올바르지 않습니다.");
        }
        return new MidiClipCreateResponse(
                request.projectId(), request.trackId(), clip.getId(), clip.getClipType(), clip.getSourceType(),
                track.getInstrument(), clip.getStartTick(), clip.getDurationTick(), clip.getStartTimeMs(),
                clip.getDurationMs(), clip.getMidiNotes(), clip.getApprovalStatus(), track.getMediaFileId());
    }

    private void validateRange(int startTick, int durationTick, long maxTick) {
        if (startTick < 0 || durationTick <= 0) {
            throw error(ProjectErrorCode.INVALID_CLIP_RANGE, "클립의 시작 위치 또는 길이가 올바르지 않습니다.");
        }
        if ((long) startTick + durationTick > maxTick) {
            throw error(ProjectErrorCode.PROJECT_DURATION_LIMIT_EXCEEDED, "곡 길이는 1분을 초과할 수 없습니다.");
        }
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
