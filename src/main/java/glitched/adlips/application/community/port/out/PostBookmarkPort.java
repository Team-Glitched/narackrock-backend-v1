package glitched.adlips.application.community.port.out;

public interface PostBookmarkPort {
    boolean lockActivePost(Long postId);
    boolean isBookmarked(Long userId, Long postId);
    void saveBookmark(Long userId, Long postId);
    void deleteBookmark(Long userId, Long postId);
}
