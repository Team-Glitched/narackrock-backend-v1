package glitched.adlips.application.view;

public record ContentViewSyncResult(
        int syncedContentCount,
        long syncedViewCount,
        int failedContentCount
) {
}
