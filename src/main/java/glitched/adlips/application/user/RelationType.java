package glitched.adlips.application.user;

public enum RelationType {
    FOLLOWING,
    FOLLOWERS;

    public static RelationType from(String value) {
        if ("following".equalsIgnoreCase(value)) {
            return FOLLOWING;
        }
        if ("followers".equalsIgnoreCase(value) || "follwers".equalsIgnoreCase(value)) {
            return FOLLOWERS;
        }
        throw new UserApplicationException(
                UserErrorCode.INVALID_RELATION_TYPE,
                "관계 유형은 following 또는 followers여야 합니다."
        );
    }
}
