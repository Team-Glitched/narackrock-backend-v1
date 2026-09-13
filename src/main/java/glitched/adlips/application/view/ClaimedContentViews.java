package glitched.adlips.application.view;

public record ClaimedContentViews(String batchId, long count) {

    public ClaimedContentViews {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
        if (count > 0 && (batchId == null || batchId.isBlank())) {
            throw new IllegalArgumentException("batchId is required when count is positive");
        }
    }

    public static ClaimedContentViews empty() {
        return new ClaimedContentViews(null, 0L);
    }

    public boolean isEmpty() {
        return count == 0;
    }
}
