package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.shorts.ShortStatus;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class PublishedShortPersistenceAdapterTest {

    @Autowired EntityManager entityManager;
    @Autowired ShortFormJpaRepository shortForms;
    @Autowired ShortParticipantJpaRepository participants;
    @Autowired ProjectMemberJpaRepository projectMembers;

    @Test
    void publishesCompletedShortWithProjectMembers() {
        User owner = User.create("owner-export@example.com");
        User editor = User.create("editor-export@example.com");
        entityManager.persist(owner);
        entityManager.persist(editor);
        Project project = new Project(owner, "완성곡", null, 701L);
        entityManager.persist(project);
        entityManager.persist(new ProjectMember(project, owner, ProjectMemberRole.OWNER));
        entityManager.persist(new ProjectMember(project, editor, ProjectMemberRole.EDITOR));
        entityManager.flush();
        PublishedShortPersistenceAdapter adapter = new PublishedShortPersistenceAdapter(
                shortForms, participants, projectMembers);

        Long shortId = adapter.publish(project, 20L, 801L);
        entityManager.flush();
        entityManager.clear();

        ShortForm saved = shortForms.findById(shortId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ShortStatus.COMPLETED);
        assertThat(saved.getExportId()).isEqualTo(20L);
        assertThat(saved.getMediaFileId()).isEqualTo(801L);
        assertThat(participants.countByShortsId(shortId)).isEqualTo(2L);
    }
}
