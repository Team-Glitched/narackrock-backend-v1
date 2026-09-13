package glitched.adlips.adapter.in.web.view;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.view.ContentViewApplicationException;
import glitched.adlips.application.view.ContentViewErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class ViewIdentityResolver {

    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ViewIdentityResolver(AuthenticatedUserResolver authenticatedUserResolver) {
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    public String resolve(String authorization, String deviceId) {
        Long userId = authenticatedUserResolver.resolveOptionalUserId(authorization);
        if (userId != null) {
            return "user:" + userId;
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new ContentViewApplicationException(
                    ContentViewErrorCode.VIEWER_ID_REQUIRED,
                    "조회자를 식별할 인증 정보 또는 기기 ID가 필요합니다."
            );
        }
        return "device:" + sha256(deviceId.trim());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
