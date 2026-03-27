package syncore.exception;

/** 낙관적 잠금(Optimistic Lock) 충돌 시 발생하는 예외 */
public class ConcurrencyException extends RuntimeException {
    public ConcurrencyException(String message) { super(message); }
}
