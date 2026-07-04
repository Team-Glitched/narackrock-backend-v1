package glitched.adlips.application.project.usecase;

import glitched.adlips.adapter.out.persistence.project.ProjectClipJpaRepository;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.MidiClipSaveRequest;
import glitched.adlips.application.project.dto.response.MidiClipSaveResponse;
import glitched.adlips.application.project.dto.request.MidiNoteRequest;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ClipType;
import glitched.adlips.domain.project.MidiNote;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MidiClipSaveUseCase {
    private final ProjectClipJpaRepository clips;
    private final Clock clock;

    @Autowired
    public MidiClipSaveUseCase(ProjectClipJpaRepository clips) {
        this(clips, Clock.systemUTC());
    }

    public MidiClipSaveUseCase(ProjectClipJpaRepository clips, Clock clock) {
        this.clips = clips;
        this.clock = clock;
    }

    @Transactional
    public MidiClipSaveResponse execute(MidiClipSaveRequest request) {
        if (request == null || request.clipId() == null || request.userId() == null
                || request.midiNotes() == null) {
            throw error(ProjectErrorCode.INVALID_MIDI_NOTE_DATA, "올바르지 않은 MIDI 노트 데이터입니다.");
        }
        var clip = clips.findByIdAndIsDeletedFalse(request.clipId())
                .orElseThrow(() -> error(ProjectErrorCode.CLIP_NOT_FOUND, "존재하지 않는 클립입니다."));
        if (!clip.getOwner().getId().equals(request.userId())) {
            throw error(ProjectErrorCode.CLIP_NOT_OWNED, "본인이 생성한 클립만 수정할 수 있습니다.");
        }
        if (clip.getClipType() != ClipType.MIDI) {
            throw error(ProjectErrorCode.CLIP_TYPE_MISMATCH, "MIDI 클립만 수정할 수 있습니다.");
        }
        if (clip.getApprovalStatus() != ApprovalStatus.DRAFT
                && clip.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw error(ProjectErrorCode.CLIP_NOT_EDITABLE, "수정할 수 없는 클립입니다.");
        }
        if (clip.getTrack().getInstrument() == null || clip.getTrack().getInstrument().isBlank()) {
            throw error(ProjectErrorCode.TRACK_INSTRUMENT_NOT_SET, "트랙의 악기가 설정되어 있지 않습니다.");
        }
        validateRange(request.startTick(), request.durationTick(), clip.getProject().getMaxTick());
        try {
            clip.updateMidi(request.startTick(), request.durationTick(),
                    request.midiNotes().stream().map(this::toDomain).toList());
        } catch (IllegalArgumentException exception) {
            throw error(ProjectErrorCode.INVALID_MIDI_NOTE_DATA, "올바르지 않은 MIDI 노트 데이터입니다.");
        }
        clips.save(clip);
        String updatedAt = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        return new MidiClipSaveResponse(
                request.clipId(), clip.getMidiNotes().size(), clip.getTrack().getMediaFileId(), updatedAt);
    }

    private MidiNote toDomain(MidiNoteRequest note) {
        return new MidiNote(note.pitch(), note.startTick(), note.durationTick(), note.velocity(),
                note.effectType(), note.effectParams());
    }

    private void validateRange(int startTick, int durationTick, long maxTick) {
        if (startTick < 0 || durationTick <= 0) {
            throw error(ProjectErrorCode.INVALID_MIDI_NOTE_DATA, "올바르지 않은 MIDI 노트 데이터입니다.");
        }
        if ((long) startTick + durationTick > maxTick) {
            throw error(ProjectErrorCode.PROJECT_DURATION_LIMIT_EXCEEDED, "곡 길이는 1분을 초과할 수 없습니다.");
        }
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
