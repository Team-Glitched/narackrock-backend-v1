package glitched.adlips.domain.project;

import java.util.Map;

public record MidiNote(
        int pitch,
        int startTick,
        int durationTick,
        int velocity,
        String effectType,
        Map<String, Object> effectParams
) {
    public MidiNote {
        if (pitch < 0 || pitch > 127) {
            throw new IllegalArgumentException("pitch must be between 0 and 127");
        }
        if (startTick < 0) {
            throw new IllegalArgumentException("startTick must not be negative");
        }
        if (durationTick <= 0) {
            throw new IllegalArgumentException("durationTick must be positive");
        }
        if (velocity < 0 || velocity > 127) {
            throw new IllegalArgumentException("velocity must be between 0 and 127");
        }
        effectParams = effectParams == null ? Map.of() : Map.copyOf(effectParams);
    }
}
