package glitched.adlips.application.community;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.community.port.out.LikedPostQueryPort;
import java.util.List;

public class GetLikedPostsUseCase {
    private final LikedPostQueryPort likedPosts;
    private final TransactionRunner transactions;
    public GetLikedPostsUseCase(LikedPostQueryPort likedPosts, TransactionRunner transactions) { this.likedPosts = likedPosts; this.transactions = transactions; }
    public List<Long> execute(Long userId) { return transactions.readOnly(() -> likedPosts.findPostIds(userId)); }
}
