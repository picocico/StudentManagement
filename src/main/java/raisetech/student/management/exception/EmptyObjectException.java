package raisetech.student.management.exception;

/**
 * リクエストボディが空オブジェクト {} の場合に投げる例外。 GlobalExceptionHandler で捕捉して E003 を返す。
 */
public class EmptyObjectException extends RuntimeException {

  public EmptyObjectException(String message) {
    super(message);
  }
}
