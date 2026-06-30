package glitched.adlips.application.port;

public interface AccessTokenVerifier {

    Long verifyAndExtractUserId(String token);
}
