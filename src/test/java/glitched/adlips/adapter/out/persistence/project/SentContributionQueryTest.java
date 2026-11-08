package glitched.adlips.adapter.out.persistence.project;

import static org.assertj.core.api.Assertions.assertThat;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class SentContributionQueryTest {
    @Autowired EntityManager entityManager;
    @Autowired ProjectContributionJpaRepository repository;

    @Test
    void queriesOnlyTheSenderAcrossActiveProjectsIncludingRejectedAndPaginatesWithoutDuplicates() {
        var owner = user("owner@test.com");
        var sender = user("sender@test.com");
        var other = user("other@test.com");
        var firstProject = project(owner, "첫 곡");
        var secondProject = project(owner, "다른 곡");
        var deletedProject = project(owner, "삭제한 곡");
        var pending = contribution(firstProject, sender);
        var rejected = contribution(secondProject, sender);
        rejected.reject(owner, null, LocalDateTime.now());
        contribution(firstProject, other);
        contribution(deletedProject, sender);
        deletedProject.delete(LocalDateTime.now());
        entityManager.flush();
        entityManager.clear();
        var all = repository.findSentByUserId(sender.getId(), null, 10);
        assertThat(all).extracting(ProjectContribution::getId).containsExactly(rejected.getId(), pending.getId());
        assertThat(all.getFirst().getApprovalStatus()).isEqualTo(ContributionApprovalStatus.REJECTED);
        assertThat(all.getFirst().getProject().getTitle()).isEqualTo("다른 곡");
        var first = repository.findSentByUserId(sender.getId(), null, 1);
        var next = repository.findSentByUserId(sender.getId(), first.getFirst().getId(), 1);
        assertThat(next).extracting(ProjectContribution::getId).containsExactly(pending.getId());
    }

    private User user(String email) {
        var user = User.create(email);
        entityManager.persist(user);
        return user;
    }
    private Project project(User owner, String title) {
        var project = new Project(owner, title, null, 701L);
        entityManager.persist(project);
        return project;
    }
    private ProjectContribution contribution(Project project, User sender) {
        var item = new ProjectContribution(project, sender, "기타 추가");
        entityManager.persist(item);
        return item;
    }
}
