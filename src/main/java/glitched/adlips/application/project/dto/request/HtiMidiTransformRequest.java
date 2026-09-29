package glitched.adlips.application.project.dto.request;

public record HtiMidiTransformRequest(
        Long projectId,
        Long userId,
        String filename,
        String contentType,
        byte[] content
) {
}
