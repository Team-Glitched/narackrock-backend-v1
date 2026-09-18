package glitched.adlips.adapter.out.persistence.project;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class ProjectContributionInboxRepositoryTest {
    @Autowired EntityManager entityManager;
    @Autowired ProjectContributionJpaRepository contributions;

    @Test
    void listsOnlyPendingRequestsForTheOwner() {
        var owner = User.create("inbox-owner@example.com");
        var anotherOwner = User.create("inbox-other@example.com");
        var contributor = User.create("inbox-contributor@example.com");
        entityManager.persist(owner);
        entityManager.persist(anotherOwner);
        entityManager.persist(contributor);
        var project = new Project(owner, "내 곡", null, 0L);
        var otherProject = new Project(anotherOwner, "다른 곡", null, 0L);
        entityManager.persist(project);
        entityManager.persist(otherProject);
        var pending = new ProjectContribution(project, contributor, "새 레이어");
        var reviewed = new ProjectContribution(project, contributor, "이전 작업");
        reviewed.approve(owner, null, LocalDateTime.now());
        entityManager.persist(pending);
        entityManager.persist(reviewed);
        entityManager.persist(new ProjectContribution(otherProject, contributor, "다른 요청"));
        entityManager.flush();
        entityManager.clear();

        var result = contributions.findForOwner(owner.getId(), ContributionApprovalStatus.PENDING,
                PageRequest.of(0, 50));

        assertThat(result).extracting(ProjectContribution::getDescription).containsExactly("새 레이어");
    }
}
