package glitched.adlips.domain.common;

/**
 * Shared Unicode code-point length limits enforced across API use cases and mirrored in the frontend.
 */
public final class ContentLimits {
    public static final int NICKNAME_MAX_LENGTH = 20;
    public static final int PROFILE_BIO_MAX_LENGTH = 200;
    public static final int INSTRUMENT_MAX_LENGTH = 50;

    public static final int COMMUNITY_NAME_MIN_LENGTH = 2;
    public static final int COMMUNITY_NAME_MAX_LENGTH = 20;
    public static final int COMMUNITY_DESCRIPTION_MAX_LENGTH = 500;

    public static final int SONG_TITLE_MAX_LENGTH = 40;
    public static final int PROJECT_DESCRIPTION_MAX_LENGTH = 500;
    public static final int TRACK_NAME_MAX_LENGTH = 40;

    public static final int SOURCE_TITLE_MAX_LENGTH = 40;
    public static final int SOURCE_DESCRIPTION_MAX_LENGTH = 500;

    public static final int POST_TITLE_MAX_LENGTH = 40;
    public static final int POST_CONTENT_MAX_LENGTH = 5000;

    public static final int COMMENT_MAX_LENGTH = 3000;
    public static final int CHAT_MESSAGE_MAX_LENGTH = 2000;

    public static final int CONTRIBUTION_COMMENT_MAX_LENGTH = 120;
    public static final int MODERATION_REASON_MAX_LENGTH = 500;
    public static final int REPORT_REASON_MAX_LENGTH = 100;
    public static final int REPORT_DESCRIPTION_MAX_LENGTH = 1000;
    public static final int SEARCH_QUERY_MAX_LENGTH = 100;

    private ContentLimits() {
    }

    public static int codePointLength(String value) {
        return value == null ? 0 : value.codePointCount(0, value.length());
    }

    public static boolean exceeds(String value, int maxLength) {
        return value != null && codePointLength(value) > maxLength;
    }
}
