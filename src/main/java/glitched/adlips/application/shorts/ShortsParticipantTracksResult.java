package glitched.adlips.application.shorts;

import glitched.adlips.application.project.dto.response.TrackGetListResponse.TrackSummary;
import java.util.List;

/** Read-only listening data; it grants no project membership or editing permission. */
public record ShortsParticipantTracksResult(Long shortId, ProjectTiming project, List<TrackSummary> tracks) {
    public record ProjectTiming(int bpm, int ppq, int maxDurationMs) {}
}
