package glitched.adlips.application.exception;

public class AlreadyRegisteredException extends RuntimeException {
    public AlreadyRegisteredException() {
        super("이미 가입된 Google 계정입니다.");
    }
}
