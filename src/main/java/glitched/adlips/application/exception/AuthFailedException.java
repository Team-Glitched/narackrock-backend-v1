package glitched.adlips.application.exception;

public class AuthFailedException extends RuntimeException {
    public AuthFailedException() {
        super("Google 계정 인증에 실패했습니다.");
    }
}
