package glitched.adlips.domain.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.user.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ShortFormTest {

    @Test
    void createsPlayableInProgressShortFromProjectExport() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "완성곡", null, 701L);

        ShortForm shortForm = ShortForm.fromExport(project, 20L, 801L);

        assertThat(shortForm.getUser()).isSameAs(owner);
        assertThat(shortForm.getProject()).isSameAs(project);
        assertThat(shortForm.getExportId()).isEqualTo(20L);
        assertThat(shortForm.getTitle()).isEqualTo("완성곡");
        assertThat(shortForm.getAlbumImageFileId()).isEqualTo(701L);
        assertThat(shortForm.getMediaFileId()).isEqualTo(801L);
        assertThat(shortForm.getStatus()).isEqualTo(ShortStatus.IN_PROGRESS);
    }

    @Test
    void createsCompletedShortWhenProjectWasAlreadyCompleted() {
        User owner = User.create("owner-completed@example.com").withId(2L);
        Project project = new Project(owner, "완성곡", null, 701L);
        project.startPublishing();
        project.publish(LocalDateTime.of(2026, 9, 16, 12, 0));
        project.complete();

        ShortForm shortForm = ShortForm.fromExport(project, 21L, 802L);

        assertThat(shortForm.getStatus()).isEqualTo(ShortStatus.COMPLETED);
    }
}
