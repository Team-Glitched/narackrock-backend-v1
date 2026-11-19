package glitched.adlips.domain.project;

import java.util.List;

/** Notes are relative to the analyzed audio region at its original analysis tempo. */
public record AudioAnalysis(AudioSoundKind soundKind, String engineVersion, String recipeVersion,
                            int bpm, int ppq, List<MidiNote> midiNotes) {
    public AudioAnalysis {
        midiNotes = List.copyOf(midiNotes);
    }
}
