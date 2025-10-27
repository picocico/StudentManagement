package raisetech.student.management.exception;

import java.io.Serial;
import lombok.Getter;

/**
 * 指定されたリソースが見つからなかった場合にスローされる例外。
 * <p>
 * 例えば、指定した学生IDやふりがなに該当するデータが存在しない場合に使用されます。
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * リソース種別（例: "student"）。null の場合もあり得る。
   */
  private final String resourceName;

  /**
   * キー名（例: "studentId"）。null の場合もあり得る。
   */
  private final String keyName;

  /**
   * リソース名・キー名からデフォルト文言を生成して例外を作成します。 例: {@code student not found: studentId}
   */
  public ResourceNotFoundException(String resourceName, String keyName) {
    this(resourceName, keyName, formatMessage(resourceName, keyName));
  }

  /**
   * リソース名・キー名・任意メッセージを指定して例外を作成します。
   */
  public ResourceNotFoundException(String resourceName, String keyName, String message) {
    super(message != null ? message : formatMessage(resourceName, keyName));
    this.resourceName = resourceName;
    this.keyName = keyName;
  }

  /**
   * メッセージのみで例外を作成します（resourceName / keyName は null）。
   */
  public ResourceNotFoundException(String message) {
    this(null, null, message);
  }

  private static String formatMessage(String resourceName, String keyName) {
    String r = resourceName != null ? resourceName : "resource";
    String k = keyName != null ? keyName : "key";
    return r + " not found: " + k;
    // 必要に応じて日本語に: return String.format("%s が見つかりません: %s", r, k);
  }
}
