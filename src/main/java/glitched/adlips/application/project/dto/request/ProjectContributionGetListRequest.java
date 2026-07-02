package glitched.adlips.application.project.dto.request;

public record ProjectContributionGetListRequest(
        Long projectId,
        Long userId,
        String status,
        Long cursor,
        int size
) {
}
