package glitched.adlips.application.project.dto.request;

public record TrackGetListRequest(Long projectId, Long userId, View view, Long contributionId) {
    public enum View { STUDIO, APPROVED, WORKING }

    public TrackGetListRequest(Long projectId, Long userId) {
        this(projectId, userId, View.WORKING, null);
    }
}
