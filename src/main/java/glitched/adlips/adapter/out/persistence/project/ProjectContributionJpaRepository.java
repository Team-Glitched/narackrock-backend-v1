package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.ProjectContribution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectContributionJpaRepository extends JpaRepository<ProjectContribution, Long> {
}
