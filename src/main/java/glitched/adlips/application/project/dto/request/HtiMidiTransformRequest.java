package glitched.adlips.application.project.dto.request;

import glitched.adlips.domain.project.AudioTranscriptionMode;

public record HtiMidiTransformRequest(
        Long projectId,
        Long userId,
        String filename,
        String contentType,
        byte[] content,
        AudioTranscriptionMode soundKind
) {
    public HtiMidiTransformRequest(Long projectId, Long userId, String filename, String contentType, byte[] content) {
        this(projectId, userId, filename, contentType, content, AudioTranscriptionMode.AUTO);
    }
}
