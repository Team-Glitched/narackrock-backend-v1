package glitched.adlips.application.community;

public enum PostSort {
    LATEST,
    POPULAR,
    OLDEST;

    public static PostSort from(String value) {
        if (value == null || value.isBlank()) {
            return LATEST;
        }
        try {
            return value.trim().toUpperCase().equals(value.trim())
                    ? valueOf(value.trim())
                    : valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new PostApplicationException(
                    PostErrorCode.INVALID_SORT_CONDITION,
                    "올바르지 않은 정렬 조건입니다. (LATEST, POPULAR, OLDEST 중 하나를 사용해주세요.)");
        }
    }
}
