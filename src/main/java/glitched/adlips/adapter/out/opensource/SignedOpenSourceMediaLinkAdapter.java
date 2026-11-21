package glitched.adlips.adapter.out.opensource;

import glitched.adlips.application.opensource.OpenSourceMediaLinkPort;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SignedOpenSourceMediaLinkAdapter implements OpenSourceMediaLinkPort {
    private final String apiBaseUrl;
    private final String shareBaseUrl;
    private final byte[] secret;
    private final long ttl;
    private final Clock clock;
    public SignedOpenSourceMediaLinkAdapter(String apiBaseUrl, String shareBaseUrl, String secret, long ttl, Clock clock) {
        this.apiBaseUrl = baseUrl(apiBaseUrl); this.shareBaseUrl = baseUrl(shareBaseUrl);
        if (secret == null || secret.length() < 32 || ttl < 1 || ttl > 86400)
            throw new IllegalArgumentException("재생 서명 키 또는 유효기간 설정이 올바르지 않습니다.");
        this.secret = secret.getBytes(StandardCharsets.UTF_8); this.ttl = ttl; this.clock = clock;
    }
    @Override public PlaybackLink playback(Long sourceId, Long mediaId) {
        long expires = clock.instant().getEpochSecond() + ttl;
        String url = apiBaseUrl + "/api/v1/sources/" + sourceId + "/audio?mediaId=" + mediaId
                + "&expires=" + expires + "&signature=" + sign(sourceId, mediaId, expires);
        return new PlaybackLink(url, java.time.Instant.ofEpochSecond(expires));
    }
    @Override public boolean verify(Long sourceId, Long mediaId, long expires, String signature) {
        if (signature == null || signature.length() != 43 || expires <= clock.instant().getEpochSecond()) return false;
        return MessageDigest.isEqual(sign(sourceId, mediaId, expires).getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII));
    }
    @Override public String share(Long sourceId) { return shareBaseUrl + "/" + sourceId; }
    private String sign(Long sourceId, Long mediaId, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(
                    ("opensource-playback:v1:" + sourceId + ":" + mediaId + ":" + expires).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) { throw new IllegalStateException(exception); }
    }
    private String baseUrl(String value) {
        URI uri = URI.create(value);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null)
            throw new IllegalArgumentException("서비스 URL 설정이 올바르지 않습니다.");
        return value.replaceAll("/+$", "");
    }
}
