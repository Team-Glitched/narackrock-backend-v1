package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortCommentPort;
import glitched.adlips.domain.shorts.ShortComment;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ShortCommentPersistenceAdapter implements ShortCommentPort {

    private final ShortFormJpaRepository shortFormRepository;
    private final ShortCommentJpaRepository shortCommentRepository;
    private final EntityManager entityManager;

    public ShortCommentPersistenceAdapter(
            ShortFormJpaRepository shortFormRepository,
            ShortCommentJpaRepository shortCommentRepository,
            EntityManager entityManager
    ) {
        this.shortFormRepository = shortFormRepository;
        this.shortCommentRepository = shortCommentRepository;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsActiveShort(Long shortId) {
        return shortFormRepository.existsByIdAndDeletedAtIsNull(shortId);
    }

    @Override
    public boolean existsActiveParentComment(Long parentCommentId, Long shortId) {
        return shortCommentRepository.existsByIdAndShortsIdAndDeletedAtIsNull(parentCommentId, shortId);
    }

    @Override
    public Long save(Long shortId, Long userId, String content, Long parentCommentId) {
        ShortForm shorts = entityManager.getReference(ShortForm.class, shortId);
        User user = entityManager.getReference(User.class, userId);
        ShortComment parent = parentCommentId == null
                ? null : entityManager.getReference(ShortComment.class, parentCommentId);
        ShortComment saved = shortCommentRepository.save(new ShortComment(shorts, user, content, parent));
        return saved.getId();
    }

    @Override
    public void adjustShortCommentCount(Long shortId, int delta) {
        shortFormRepository.adjustCommentCount(shortId, delta);
    }

    @Override
    public void adjustParentReplyCount(Long parentCommentId, int delta) {
        shortCommentRepository.adjustReplyCount(parentCommentId, delta);
    }

    @Override
    public Optional<ShortComment> findActiveComment(Long commentId, Long shortId) {
        return shortCommentRepository.findByIdAndShortsIdAndDeletedAtIsNull(commentId, shortId);
    }

    @Override
    public void updateContent(ShortComment comment) {
        shortCommentRepository.save(comment);
    }
}
