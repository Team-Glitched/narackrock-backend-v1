package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.AudioSoundKind;
import java.util.List;
import java.util.Map;

public interface HtiMidiTransformPort {
    Result transform(Command command);

    default AudioResult analyze(AudioCommand command) {
        throw new HtiServiceException("Audio analysis is unavailable", null);
    }

    record AudioCommand(AudioSoundKind soundKind, String filename, String contentType, byte[] content,
                        int bpm, int ppq, int offsetMs, long durationMs) {}

    record AudioResult(String engineVersion, List<Note> midiNotes) {}

    record Command(
            long clipId,
            String filename,
            String contentType,
            byte[] content,
            int bpm,
            int ppq
    ) {
    }

    record Result(int durationTick, List<Note> midiNotes) {
    }

    record Note(
            int pitch,
            int startTick,
            int durationTick,
            int velocity,
            String effectType,
            Map<String, Object> effectParams
    ) {
    }

    class HtiServiceException extends RuntimeException {
        public HtiServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
