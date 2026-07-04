package glitched.adlips.application.project.dto.response;

public record MidiClipSaveResponse(
        Long clipId,
        int noteCount,
        Long trackMediaFileId,
        String updatedAt
) {
}
