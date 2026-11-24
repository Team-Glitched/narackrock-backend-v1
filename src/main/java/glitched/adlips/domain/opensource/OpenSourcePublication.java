package glitched.adlips.domain.opensource;

import java.time.Instant;

public record OpenSourcePublication(Long id, Long ownerId, String title, String description,
        Long mediaFileId, Long thumbnailMediaFileId, boolean isPublic,
        Instant createdAt, Instant updatedAt, Instant deletedAt, String instrument,
        String midiNotesJson, Integer midiPpq, Integer midiBpm) {
    public OpenSourcePublication(Long id, Long ownerId, String title, String description,
            Long mediaFileId, Long thumbnailMediaFileId, boolean isPublic,
            Instant createdAt, Instant updatedAt, Instant deletedAt, String instrument) {
        this(id, ownerId, title, description, mediaFileId, thumbnailMediaFileId, isPublic,
                createdAt, updatedAt, deletedAt, instrument, null, null, null);
    }

    public boolean hasMidiNotes() {
        return midiNotesJson != null && !midiNotesJson.isBlank();
    }

    public OpenSourcePublication edit(String title, String description, Long thumbnailMediaFileId, String instrument, Instant now) {
        return new OpenSourcePublication(id, ownerId, title, description, mediaFileId,
                thumbnailMediaFileId, isPublic, createdAt, now, deletedAt, instrument,
                midiNotesJson, midiPpq, midiBpm);
    }
    public OpenSourcePublication visibility(boolean visible, Instant now) {
        return new OpenSourcePublication(id, ownerId, title, description, mediaFileId,
                thumbnailMediaFileId, visible, createdAt, visible == isPublic ? updatedAt : now, deletedAt, instrument,
                midiNotesJson, midiPpq, midiBpm);
    }
    public OpenSourcePublication delete(Instant now) {
        if (deletedAt != null) return this;
        return new OpenSourcePublication(id, ownerId, title, description, mediaFileId,
                thumbnailMediaFileId, false, createdAt, now, now, instrument,
                midiNotesJson, midiPpq, midiBpm);
    }
}
