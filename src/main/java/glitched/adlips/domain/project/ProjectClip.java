package glitched.adlips.domain.project;

import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "project_clips", indexes = {
        @Index(name = "idx_clips_project_start", columnList = "project_id,start_tick"),
        @Index(name = "idx_clips_track_start", columnList = "track_id,start_tick"),
        @Index(name = "idx_clips_track_approval", columnList = "track_id,approval_status"),
        @Index(name = "idx_clips_owner", columnList = "owner_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectClip extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private ProjectTrack track;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "clip_type", nullable = false)
    private ClipType clipType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private ClipSourceType sourceType;

    @Column(name = "media_file_id")
    private Long mediaFileId;

    @Column(name = "waveform_file_id")
    private Long waveformFileId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "midi_notes", columnDefinition = "json")
    private List<MidiNote> midiNotes = List.of();

    @Column(name = "start_tick", nullable = false)
    private int startTick;

    @Column(name = "duration_tick")
    private Integer durationTick;

    @Column(name = "start_time_ms", nullable = false)
    private int startTimeMs;

    @Column(name = "clip_offset_ms", nullable = false)
    private int clipOffsetMs;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.DRAFT;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    private ProjectClip(Project project, ProjectTrack track, User owner,
                        ClipType clipType, ClipSourceType sourceType,
                        int startTick, int durationTick, int sortOrder) {
        validateProjectAndTimeline(project, track, startTick, durationTick);
        this.project = Objects.requireNonNull(project, "project must not be null");
        this.track = Objects.requireNonNull(track, "track must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.clipType = Objects.requireNonNull(clipType, "clipType must not be null");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.startTick = startTick;
        this.durationTick = durationTick;
        this.startTimeMs = project.tickToMilliseconds(startTick);
        this.durationMs = project.tickToMilliseconds(durationTick);
        this.sortOrder = sortOrder;
    }

    public static ProjectClip createAudio(
            Project project, ProjectTrack track, User owner, ClipSourceType sourceType,
            Long mediaFileId, Long waveformFileId, int startTick, int durationTick,
            int clipOffsetMs, int durationMs, int sortOrder) {
        validateAudioData(sourceType, mediaFileId, clipOffsetMs, durationMs);
        ProjectClip clip = new ProjectClip(
                project, track, owner, ClipType.AUDIO, sourceType,
                startTick, durationTick, sortOrder);
        clip.mediaFileId = mediaFileId;
        clip.waveformFileId = waveformFileId;
        clip.clipOffsetMs = clipOffsetMs;
        clip.durationMs = durationMs;
        clip.midiNotes = List.of();
        track.invalidateRenderedMedia();
        return clip;
    }

    public static ProjectClip createMidi(
            Project project, ProjectTrack track, User owner,
            int startTick, int durationTick, List<MidiNote> midiNotes, int sortOrder) {
        Objects.requireNonNull(track, "track must not be null");
        if (track.getInstrument() == null || track.getInstrument().isBlank()) {
            throw new IllegalStateException("track instrument must be set for a MIDI clip");
        }
        List<MidiNote> notes = List.copyOf(Objects.requireNonNull(midiNotes, "midiNotes must not be null"));
        validateMidiNotes(notes, durationTick);
        ProjectClip clip = new ProjectClip(
                project, track, owner, ClipType.MIDI, ClipSourceType.MIDI_INPUT,
                startTick, durationTick, sortOrder);
        clip.midiNotes = notes;
        track.invalidateRenderedMedia();
        return clip;
    }

    public void updateAudio(
            ClipSourceType sourceType, Long mediaFileId, Long waveformFileId,
            int startTick, int durationTick, int clipOffsetMs, int durationMs) {
        if (clipType != ClipType.AUDIO) {
            throw new IllegalStateException("only AUDIO clips can update audio data");
        }
        validateAudioData(sourceType, mediaFileId, clipOffsetMs, durationMs);
        validateProjectAndTimeline(project, track, startTick, durationTick);
        this.sourceType = sourceType;
        this.mediaFileId = mediaFileId;
        this.waveformFileId = waveformFileId;
        this.startTick = startTick;
        this.durationTick = durationTick;
        this.startTimeMs = project.tickToMilliseconds(startTick);
        this.durationMs = durationMs;
        this.clipOffsetMs = clipOffsetMs;
        this.midiNotes = List.of();
        track.invalidateRenderedMedia();
    }

    public void updateMidi(int startTick, int durationTick, List<MidiNote> midiNotes) {
        if (clipType != ClipType.MIDI) {
            throw new IllegalStateException("only MIDI clips can update MIDI notes");
        }
        validateProjectAndTimeline(project, track, startTick, durationTick);
        List<MidiNote> notes = List.copyOf(Objects.requireNonNull(midiNotes, "midiNotes must not be null"));
        validateMidiNotes(notes, durationTick);
        this.startTick = startTick;
        this.durationTick = durationTick;
        this.startTimeMs = project.tickToMilliseconds(startTick);
        this.durationMs = project.tickToMilliseconds(durationTick);
        this.midiNotes = notes;
        track.invalidateRenderedMedia();
    }

    public void submitForReview() {
        if (approvalStatus != ApprovalStatus.DRAFT) {
            throw new IllegalStateException("only draft clip can be submitted");
        }
        approvalStatus = ApprovalStatus.PENDING;
    }

    public void approve() {
        approvalStatus = ApprovalStatus.APPROVED;
    }

    public void reject() {
        approvalStatus = ApprovalStatus.REJECTED;
    }

    private static void validateProjectAndTimeline(
            Project project, ProjectTrack track, int startTick, int durationTick) {
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(track, "track must not be null");
        if (!track.belongsTo(project)) {
            throw new IllegalArgumentException("track and clip must belong to the same project");
        }
        if (startTick < 0 || durationTick <= 0
                || (long) startTick + durationTick > project.getMaxTick()) {
            throw new IllegalArgumentException("clip range exceeds the project timeline");
        }
    }

    private static void validateMidiNotes(List<MidiNote> notes, int clipDurationTick) {
        boolean outsideClip = notes.stream()
                .anyMatch(note -> (long) note.startTick() + note.durationTick() > clipDurationTick);
        if (outsideClip) {
            throw new IllegalArgumentException("MIDI note exceeds the clip timeline");
        }
    }

    private static void validateAudioData(
            ClipSourceType sourceType, Long mediaFileId, int clipOffsetMs, int durationMs) {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(mediaFileId, "mediaFileId must not be null");
        if (sourceType == ClipSourceType.MIDI_INPUT) {
            throw new IllegalArgumentException("MIDI_INPUT is not an audio source type");
        }
        if (clipOffsetMs < 0 || durationMs <= 0) {
            throw new IllegalArgumentException("audio offset and duration are invalid");
        }
    }
}
