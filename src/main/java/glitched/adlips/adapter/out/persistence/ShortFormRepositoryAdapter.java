package glitched.adlips.adapter.out.persistence;

import glitched.adlips.application.port.ShortFormRepository;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.shorts.ShortStatus;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class ShortFormRepositoryAdapter implements ShortFormRepository {

    private final ShortFormJpaRepository jpaRepository;

    ShortFormRepositoryAdapter(ShortFormJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<ShortForm> findByCursor(Long cursor, ShortStatus status, int limit) {
        if (cursor == null) {
            return jpaRepository.findFirstPage(status, limit);
        }
        return jpaRepository.findNextPage(cursor, status, limit);
    }
}
