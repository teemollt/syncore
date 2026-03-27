package syncore.exception;

/** 권한 부족 시 발생하는 예외 */
public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) { super(message); }
}
