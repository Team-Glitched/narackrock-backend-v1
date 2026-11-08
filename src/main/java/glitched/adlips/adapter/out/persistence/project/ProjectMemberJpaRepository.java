package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.ProjectMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberJpaRepository extends JpaRepository<ProjectMember, Long> {
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMember> findByProjectIdOrderByIdAsc(Long projectId);

    @Query("""
            select m from ProjectMember m
            where m.project.id = :projectId
              and (m.user.id = m.project.owner.id or exists (
                  select c.id from ProjectContribution c
                  where c.project.id = :projectId and c.user.id = m.user.id
                    and c.approvalStatus = glitched.adlips.domain.project.ContributionApprovalStatus.APPROVED
              ))
            order by m.id asc
            """)
    List<ProjectMember> findPublishedContributors(@Param("projectId") Long projectId);
}
