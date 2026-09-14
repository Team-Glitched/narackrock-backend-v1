package glitched.adlips.application.project.port.out;

import java.util.List;

public interface ProjectAudioMixerPort {

    MixedAudio mix(List<AudioLayer> layers);

    record AudioLayer(
            byte[] content,
            int timelineStartMs,
            int sourceOffsetMs,
            int durationMs,
            int volume,
            int pan
    ) {
    }

    record MixedAudio(byte[] content, int durationMs) {
    }
}
