package glitched.adlips.application.shorts.port.out;

public interface ShortBookmarkPort {
    boolean isBookmarked(Long userId, Long shortId);
    void saveBookmark(Long userId, Long shortId);
    void deleteBookmark(Long userId, Long shortId);
}
