package glitched.adlips.adapter.out.persistence;

import glitched.adlips.application.port.ShortCommentRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
class ShortCommentRepositoryAdapter implements ShortCommentRepository {

    private final ShortCommentJpaRepository jpaRepository;

    ShortCommentRepositoryAdapter(ShortCommentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Map<Long, Long> countByShortIds(List<Long> shortIds) {
        if (shortIds.isEmpty()) {
            return Map.of();
        }
        return jpaRepository.countByShortIdIn(shortIds).stream()
                .collect(Collectors.toMap(
                        ShortCommentJpaRepository.CommentCount::getShortId,
                        ShortCommentJpaRepository.CommentCount::getCount
                ));
    }
}
