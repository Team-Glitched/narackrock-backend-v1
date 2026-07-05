package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem;

public record ShortsCompositionResponse(
        Long shortId,
        Long projectId,
        String compositionUrl,
        String title,
        String status
) {
    public static ShortsCompositionResponse from(ShortsCompositionQueryItem item) {
        return new ShortsCompositionResponse(
                item.shortId(),
                item.projectId(),
                "/composition/projects/" + item.projectId(),
                item.projectTitle(),
                item.projectStatus().name()
        );
    }
}
