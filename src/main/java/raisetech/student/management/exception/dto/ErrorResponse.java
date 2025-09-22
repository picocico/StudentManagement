package raisetech.student.management.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "エラーレスポンスの共通形式")
public class ErrorResponse {

  @Schema(description = "HTTPステータスコード", example = "400")
  private int status;

  @Schema(description = "エラーコード（定義された識別ID）", example = "E001")
  private String code;

  @Schema(description = "エラー種別（識別用の文字列）", example = "VALIDATION_FAILED")
  private String error;

  @Schema(description = "エラーメッセージ", example = "入力値に不備があります")
  private String message;

  @Schema(description = "フィールドごとのバリデーションエラー詳細", nullable = true)
  private List<FieldErrorDetail> errors;

  // テスト互換: details = errors のエイリアスとして同じ内容を格納
  @Schema(description = "errors と同義の互換フィールド", nullable = true)
  private List<FieldErrorDetail> details;

  // 便利ファクトリ
  public static ErrorResponse of(int status, String error, String code, String message) {
    return of(status, error, code, message, null, null);
  }

  public static ErrorResponse of(
      int status, String error, String code, String message,
      List<FieldErrorDetail> errors, List<FieldErrorDetail> details) {
    return new ErrorResponse(status, code, error, message, errors, details != null ? details : errors);
  }
}
