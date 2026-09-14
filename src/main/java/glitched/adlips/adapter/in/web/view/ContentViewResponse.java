package glitched.adlips.adapter.in.web.view;

import glitched.adlips.application.view.ContentViewResult;

public record ContentViewResponse(Long contentId, boolean counted) {

    static ContentViewResponse from(ContentViewResult result) {
        return new ContentViewResponse(result.contentId(), result.counted());
    }
}
