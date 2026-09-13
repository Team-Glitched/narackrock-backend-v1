package glitched.adlips.application.view;

import java.util.Objects;

public record ContentViewKey(ContentViewTarget target, Long contentId) {

    public ContentViewKey {
        Objects.requireNonNull(target, "target must not be null");
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("contentId must be positive");
        }
    }
}
