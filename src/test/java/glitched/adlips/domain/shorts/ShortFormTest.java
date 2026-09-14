package glitched.adlips.domain.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.user.User;
import org.junit.jupiter.api.Test;

class ShortFormTest {

    @Test
    void createsCompletedShortFromProjectExport() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "완성곡", null, 701L);

        ShortForm shortForm = ShortForm.completedFromExport(project, 20L, 801L);

        assertThat(shortForm.getUser()).isSameAs(owner);
        assertThat(shortForm.getProject()).isSameAs(project);
        assertThat(shortForm.getExportId()).isEqualTo(20L);
        assertThat(shortForm.getTitle()).isEqualTo("완성곡");
        assertThat(shortForm.getAlbumImageFileId()).isEqualTo(701L);
        assertThat(shortForm.getMediaFileId()).isEqualTo(801L);
        assertThat(shortForm.getStatus()).isEqualTo(ShortStatus.COMPLETED);
    }
}
