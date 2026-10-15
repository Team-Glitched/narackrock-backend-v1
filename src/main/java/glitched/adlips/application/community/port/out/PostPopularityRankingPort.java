package glitched.adlips.application.community.port.out;

import java.util.List;

public interface PostPopularityRankingPort {
    void seedBaseline(List<PostPopularityScore> scores);
    List<Long> findTop(int limit);
    void updateLikeCount(Long postId, int likeCount);
    void remove(Long postId);
}
