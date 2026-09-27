package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.PostBookmarkPort;
import org.springframework.stereotype.Repository;

@Repository
public class PostBookmarkPersistenceAdapter implements PostBookmarkPort {

    private final PostJpaRepository postRepository;
    private final PostBookmarkJpaRepository bookmarkRepository;

    public PostBookmarkPersistenceAdapter(
            PostJpaRepository postRepository,
            PostBookmarkJpaRepository bookmarkRepository
    ) {
        this.postRepository = postRepository;
        this.bookmarkRepository = bookmarkRepository;
    }

    @Override
    public boolean lockActivePost(Long postId) {
        return postRepository.findActiveByIdForUpdate(postId).isPresent();
    }

    @Override
    public boolean isBookmarked(Long userId, Long postId) {
        return bookmarkRepository.existsByUser_IdAndPost_Id(userId, postId);
    }

    @Override
    public void saveBookmark(Long userId, Long postId) {
        bookmarkRepository.insertBookmark(userId, postId);
    }

    @Override
    public void deleteBookmark(Long userId, Long postId) {
        bookmarkRepository.deleteBookmark(userId, postId);
    }
}
