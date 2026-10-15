package glitched.adlips.application.community.port.out;

public record PostPopularityScore(Long postId, int viewCount, int likeCount) {
    public double recencyTieBreaker() {
        return Math.min(postId, 999_999_999L) / 1_000_000_000_000.0;
    }
}
