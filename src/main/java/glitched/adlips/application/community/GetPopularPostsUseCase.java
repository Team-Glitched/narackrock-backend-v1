package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostQueryPort;
import glitched.adlips.application.community.port.out.PostQueryItem;
import glitched.adlips.application.community.port.out.PostPopularityRankingPort;
import glitched.adlips.application.port.TransactionRunner;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 갤러리 전체 게시글을 Redis의 좋아요 × 5 + 조회수 순위로 조회한다. */
public class GetPopularPostsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final PostQueryPort port;
    private final PostPopularityRankingPort ranking;
    private final TransactionRunner transactionRunner;

    public GetPopularPostsUseCase(PostQueryPort port, PostPopularityRankingPort ranking, TransactionRunner transactionRunner) {
        this.port = port;
        this.ranking = ranking;
        this.transactionRunner = transactionRunner;
    }

    public PopularPostListResult execute(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE || (long) page * size > Integer.MAX_VALUE) {
            throw new PostApplicationException(PostErrorCode.VALIDATION_ERROR, "페이지 번호와 크기를 확인해 주세요.");
        }
        return transactionRunner.readOnly(() -> {
            ranking.seedBaseline(port.findPopularityScores());
            int from = page * size;
            long totalCount = Math.min(port.countAllPosts(), MAX_PAGE_SIZE);
            if (from >= MAX_PAGE_SIZE) return new PopularPostListResult(page, size, totalCount, List.of());
            List<Long> ids = ranking.findTop(Math.min(from + size, MAX_PAGE_SIZE));
            if (from >= ids.size()) return new PopularPostListResult(page, size, totalCount, List.of());
            List<Long> selected = ids.subList(from, ids.size());
            Map<Long, PostQueryItem> byId = port.findByIds(selected).stream()
                    .collect(Collectors.toMap(PostQueryItem::postId, Function.identity()));
            List<PostQueryItem> ordered = selected.stream().map(byId::get)
                    .filter(java.util.Objects::nonNull).toList();
            return new PopularPostListResult(page, size, totalCount, ordered);
        });
    }
}
