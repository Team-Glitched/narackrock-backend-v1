package glitched.adlips.application.view;

public record ContentViewResult(
        ContentViewTarget target,
        Long contentId,
        boolean counted
) {
}
