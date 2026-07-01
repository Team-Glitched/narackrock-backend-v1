package glitched.adlips.application.user.account.port.out;

public interface RefreshTokenGeneratorPort {
    String generate();

    String hash(String token);
}
