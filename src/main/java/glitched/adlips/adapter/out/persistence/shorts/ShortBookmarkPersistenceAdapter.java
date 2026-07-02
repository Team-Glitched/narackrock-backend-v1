package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortBookmarkPort;
import org.springframework.stereotype.Repository;

@Repository
public class ShortBookmarkPersistenceAdapter implements ShortBookmarkPort {

    private final ShortBookmarkJpaRepository bookmarkRepository;

    public ShortBookmarkPersistenceAdapter(ShortBookmarkJpaRepository bookmarkRepository) {
        this.bookmarkRepository = bookmarkRepository;
    }

    @Override
    public boolean isBookmarked(Long userId, Long shortId) {
        return bookmarkRepository.existsByUser_IdAndShorts_Id(userId, shortId);
    }

    @Override
    public void saveBookmark(Long userId, Long shortId) {
        bookmarkRepository.insertBookmark(userId, shortId);
    }

    @Override
    public void deleteBookmark(Long userId, Long shortId) {
        bookmarkRepository.deleteBookmark(userId, shortId);
    }
}
