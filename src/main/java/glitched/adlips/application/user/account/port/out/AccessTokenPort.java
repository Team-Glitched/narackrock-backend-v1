package glitched.adlips.application.user.account.port.out;

public interface AccessTokenPort {
    String issue(Long userId);

    Long verify(String token);
}
