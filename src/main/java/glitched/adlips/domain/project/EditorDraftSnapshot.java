package glitched.adlips.domain.project;

import java.util.List;
import java.util.Objects;

public record EditorDraftSnapshot(int startTick, long durationTick, List<MidiNote> midiNotes) {
    public EditorDraftSnapshot {
        midiNotes = List.copyOf(Objects.requireNonNull(midiNotes));
        if (startTick < 0 || durationTick <= 0 || midiNotes.size() > 10_000
                || midiNotes.stream().anyMatch(note -> (long) note.startTick() + note.durationTick() > durationTick)) {
            throw new IllegalArgumentException("invalid MIDI draft timeline");
        }
    }

    public void validateMaxTick(long maxTick) {
        if (durationTick > maxTick - startTick) {
            throw new IllegalArgumentException("MIDI draft exceeds project timeline");
        }
    }

    public static EditorDraftSnapshot from(ProjectClip clip) {
        return new EditorDraftSnapshot(clip.getStartTick(), clip.getDurationTick(), clip.getMidiNotes());
    }
}
