package glitched.adlips.application.community.port.out;

import java.util.List;

public interface LikedPostQueryPort {
    List<Long> findPostIds(Long userId);
}
