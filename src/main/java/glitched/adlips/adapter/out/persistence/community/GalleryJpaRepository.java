package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.domain.community.Gallery;
import org.springframework.data.jpa.repository.JpaRepository;

interface GalleryJpaRepository extends JpaRepository<Gallery, Long> {
}
