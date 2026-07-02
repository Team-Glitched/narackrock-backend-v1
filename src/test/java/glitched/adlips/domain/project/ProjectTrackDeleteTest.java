package glitched.adlips.domain.project;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.user.User;
import org.junit.jupiter.api.Test;

class ProjectTrackDeleteTest {
    @Test
    void draftTrackDeletionKeepsProjectVersionAndInvalidatesMedia() {
        User owner = User.create("owner@example.com");
        Project project = new Project(owner, "곡", null, 701L);
        ProjectTrack track = new ProjectTrack(project, owner, "트랙", "GUITAR", 0);
        track.replaceRenderedMedia(601L);

        track.delete();

        assertThat(track.isDeleted()).isTrue();
        assertThat(track.getMediaFileId()).isNull();
        assertThat(project.getDisplayVersion()).isEqualTo("v1.1");
    }
}
