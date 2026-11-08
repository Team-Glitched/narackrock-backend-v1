package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.shorts.ShortParticipant;
import glitched.adlips.domain.shorts.ShortStatus;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
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
    void excludesEditorsWhoHaveNotSubmittedApprovedContributions() {
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
        assertThat(saved.getStatus()).isEqualTo(ShortStatus.IN_PROGRESS);
        assertThat(saved.getExportId()).isEqualTo(20L);
        assertThat(saved.getMediaFileId()).isEqualTo(801L);
        assertThat(participants.findByShortsId(shortId))
                .extracting(it -> it.getUser().getId()).containsExactly(owner.getId());

        adapter.completeProject(project.getId());
        entityManager.flush();
        entityManager.clear();
        assertThat(shortForms.findById(shortId).orElseThrow().getStatus())
                .isEqualTo(ShortStatus.COMPLETED);
    }

    @Test
    void refreshesExistingInProgressShortWhenProjectIsExportedAgain() {
        User owner = User.create("owner-refresh@example.com");
        entityManager.persist(owner);
        Project project = new Project(owner, "작업 중", null, 701L);
        entityManager.persist(project);
        entityManager.persist(new ProjectMember(project, owner, ProjectMemberRole.OWNER));
        entityManager.flush();
        PublishedShortPersistenceAdapter adapter = new PublishedShortPersistenceAdapter(
                shortForms, participants, projectMembers);

        Long firstId = adapter.publish(project, 20L, 801L);
        Long refreshedId = adapter.publish(project, 21L, 802L);
        entityManager.flush();
        entityManager.clear();

        assertThat(refreshedId).isEqualTo(firstId);
        ShortForm saved = shortForms.findById(firstId).orElseThrow();
        assertThat(saved.getExportId()).isEqualTo(21L);
        assertThat(saved.getMediaFileId()).isEqualTo(802L);
        assertThat(participants.countByShortsId(firstId)).isEqualTo(1L);
    }

    @Test
    void refreshKeepsOnlyOwnerAndApprovedContributorsAndRemovesStaleParticipants() {
        User owner = User.create("owner-eligibility@example.com");
        User approved = User.create("approved-eligibility@example.com");
        User pending = User.create("pending-eligibility@example.com");
        User rejected = User.create("rejected-eligibility@example.com");
        User visitor = User.create("visitor-eligibility@example.com");
        for (User user : List.of(owner, approved, pending, rejected, visitor)) {
            entityManager.persist(user);
        }
        Project project = new Project(owner, "참여자 검증", null, 701L);
        entityManager.persist(project);
        for (User user : List.of(owner, approved, pending, rejected, visitor)) {
            entityManager.persist(new ProjectMember(project, user,
                    user == owner ? ProjectMemberRole.OWNER : ProjectMemberRole.EDITOR));
        }
        ProjectContribution accepted = new ProjectContribution(project, approved, "승인됨");
        accepted.approve(owner, null, LocalDateTime.now());
        entityManager.persist(accepted);
        entityManager.persist(new ProjectContribution(project, pending, "대기 중"));
        ProjectContribution declined = new ProjectContribution(project, rejected, "거절됨");
        declined.reject(owner, null, LocalDateTime.now());
        entityManager.persist(declined);
        ShortForm shortForm = shortForms.save(ShortForm.fromExport(project, 10L, 801L));
        participants.save(new ShortParticipant(shortForm, visitor, "EDITOR"));
        entityManager.flush();
        PublishedShortPersistenceAdapter adapter = new PublishedShortPersistenceAdapter(
                shortForms, participants, projectMembers);

        Long shortId = adapter.publish(project, 20L, 802L);
        adapter.publish(project, 21L, 803L);
        entityManager.flush();
        entityManager.clear();

        assertThat(shortId).isEqualTo(shortForm.getId());
        assertThat(participants.findByShortsId(shortId))
                .extracting(it -> it.getUser().getId())
                .containsExactlyInAnyOrder(owner.getId(), approved.getId());
    }
}
