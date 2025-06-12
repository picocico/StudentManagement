package raisetech.student.management.exception.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * バリデーションエラーの各フィールドに関する詳細情報を格納する DTO クラス。
 * <p>
 * 主に {@link org.springframework.web.bind.MethodArgumentNotValidException} の例外処理時に、
 * 各フィールドの入力エラー内容をクライアントに返す際に使用されます。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorDetail {

  /**
   * エラーが発生したフィールド名。
   */
  private String field;

  /**
   * エラーに対応するメッセージ内容。
   */
  private String message;
}
