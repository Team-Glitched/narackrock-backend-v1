package glitched.adlips.application.project.port.out;

import java.util.List;
import java.util.Map;

public interface HtiMidiTransformPort {
    Result transform(Command command);

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
