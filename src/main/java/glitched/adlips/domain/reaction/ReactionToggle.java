package glitched.adlips.domain.reaction;

import java.util.Objects;

/**
 * 좋아요/싫어요 토글 규칙. 같은 반응을 다시 누르면 취소, 반대 반응을 누르면 전환한다.
 * result가 null이면 토글 후 반응이 없는 상태다.
 */
public record ReactionToggle(ReactionType result, int likeDelta, int dislikeDelta) {

    public static ReactionToggle of(ReactionType current, ReactionType requested) {
        Objects.requireNonNull(requested, "requested must not be null");
        ReactionType result = current == requested ? null : requested;
        return new ReactionToggle(
                result,
                delta(current, result, ReactionType.LIKE),
                delta(current, result, ReactionType.DISLIKE));
    }

    private static int delta(ReactionType before, ReactionType after, ReactionType type) {
        return (after == type ? 1 : 0) - (before == type ? 1 : 0);
    }
}
