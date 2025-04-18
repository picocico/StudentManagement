package raisetech.student.management.exception;

/**
 * リソースが存在しない場合にスローされる例外。
 */
public class ResourceNotFoundException extends RuntimeException {

  /**
   * 指定されたメッセージで新しい例外を生成します。
   *
   * @param message エラーメッセージ
   */
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
