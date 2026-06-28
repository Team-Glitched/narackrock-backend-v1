package glitched.adlips.application.exception;

public class SignupRequiredException extends RuntimeException {
    public SignupRequiredException() {
        super("가입되지 않은 계정입니다.");
    }
}
