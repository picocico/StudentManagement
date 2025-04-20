package raisetech.student.management.exception;

/**
 * 指定されたリソースが見つからなかった場合にスローされる例外。
 * <p>
 * 例えば、指定した学生IDやふりがなに該当するデータが存在しない場合に使用されます。
 */
public class ResourceNotFoundException extends RuntimeException {

  /**
   * メッセージを指定して例外を生成します。
   *
   * @param message エラーメッセージ
   */
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
