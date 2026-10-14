package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.domain.community.GalleryManager;
import org.springframework.data.jpa.repository.JpaRepository;

interface GalleryManagerJpaRepository extends JpaRepository<GalleryManager, Long> {
}
