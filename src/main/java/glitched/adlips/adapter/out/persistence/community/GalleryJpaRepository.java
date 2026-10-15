package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.domain.community.Gallery;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface GalleryJpaRepository extends JpaRepository<Gallery, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Gallery g WHERE g.id = :galleryId")
    Optional<Gallery> findByIdForUpdate(@Param("galleryId") Long galleryId);

    boolean existsByName(String name);
}
