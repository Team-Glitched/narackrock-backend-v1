package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.ExportStatus;
import glitched.adlips.domain.project.ProjectExport;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectExportJpaRepository extends JpaRepository<ProjectExport, Long> {

    Optional<ProjectExport> findExportByIdAndProjectId(Long id, Long projectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select export from ProjectExport export where export.id = :id")
    Optional<ProjectExport> findExportByIdForUpdate(@Param("id") Long id);

    @Query("select export.id from ProjectExport export where export.status = :status order by export.id")
    List<Long> findIdsByStatus(@Param("status") ExportStatus status, Pageable pageable);
}
