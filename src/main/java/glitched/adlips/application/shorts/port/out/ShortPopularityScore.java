package glitched.adlips.application.shorts.port.out;

public record ShortPopularityScore(Long shortId, int viewCount, int likeCount) {
    public double recencyTieBreaker() {
        return Math.min(shortId, 999_999_999L) / 1_000_000_000_000.0;
    }
}
