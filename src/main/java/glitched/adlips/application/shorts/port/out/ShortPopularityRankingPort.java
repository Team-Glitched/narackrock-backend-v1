package glitched.adlips.application.shorts.port.out;

import java.util.List;
import glitched.adlips.application.shorts.port.out.ShortPopularityScore;

public interface ShortPopularityRankingPort {
    List<Long> findTop(int limit);

    void seedBaseline(List<ShortPopularityScore> scores);

    void incrementView(Long shortId);

    void updateLikeCount(Long shortId, int likeCount);
}
