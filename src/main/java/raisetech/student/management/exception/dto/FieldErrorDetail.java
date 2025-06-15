package raisetech.student.management.exception.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "バリデーションエラー時のフィールドごとの詳細")
public class FieldErrorDetail {

  /**
   * エラーが発生したフィールド名。
   */
  @Schema(description = "エラーが発生したフィールド名", example = "email")
  private String field;

  /**
   * エラーに対応するメッセージ内容。
   */
  @Schema(description = "そのフィールドに関するエラーメッセージ",
      example = "メールアドレスの形式が不正です")
  private String message;
}
