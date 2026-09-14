package glitched.adlips.application.shorts;

public record ShortReactionCounts(int likeCount, int dislikeCount) {

    public ShortReactionCounts {
        if (likeCount < 0 || dislikeCount < 0) {
            throw new IllegalArgumentException("반응 수는 음수일 수 없습니다.");
        }
    }
}
