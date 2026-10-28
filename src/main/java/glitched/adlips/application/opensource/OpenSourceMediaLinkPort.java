package glitched.adlips.application.opensource;

import java.time.Instant;

public interface OpenSourceMediaLinkPort {
    record PlaybackLink(String url, Instant expiresAt) {}
    PlaybackLink playback(Long sourceId, Long mediaId);
    boolean verify(Long sourceId, Long mediaId, long expires, String signature);
    String share(Long sourceId);
}
