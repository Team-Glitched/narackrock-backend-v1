package glitched.adlips.application.project.dto.request;

import java.util.Map;

public record MidiNoteRequest(
        int pitch,
        int startTick,
        int durationTick,
        int velocity,
        String effectType,
        Map<String, Object> effectParams
) {
}
