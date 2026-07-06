package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.Project;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectJpaRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Project> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);
}
