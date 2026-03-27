package syncore.exception;

/** 비즈니스 규칙 위반 시 발생하는 예외 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
