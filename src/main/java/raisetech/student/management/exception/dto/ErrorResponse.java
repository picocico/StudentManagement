package raisetech.student.management.exception.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 共通エラーレスポンス構造。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "エラーレスポンスの共通形式")
public class ErrorResponse {

  @Schema(description = "HTTPステータスコード", example = "400")
  private int status;       // "error"

  @Schema(description = "アプリケーション固有のステータスコード", example = "400")
  private int code;            // HTTPステータスコード

  @Schema(description = "エラー種別（識別用の文字列）", example = "VALIDATION_FAILED")
  private String errorType;    // アプリケーション分類

  @Schema(description = "エラーコード（定義された識別ID）", example = "E001")
  private String errorCode;    // アプリケーション固有コード

  @Schema(description = "エラーメッセージ", example = "入力値に不備があります")
  private String message;      // ユーザー向けメッセージ

  @Schema(description = "フィールドごとのバリデーションエラー詳細", nullable = true)
  private List<FieldErrorDetail> errors; // フィールド単位のエラー（省略可）

}
