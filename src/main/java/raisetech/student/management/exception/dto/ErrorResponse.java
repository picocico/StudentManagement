package raisetech.student.management.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ErrorResponse", description = "APIの共通エラーレスポンス",
    example = """
        {
          "status": 400,
          "code": "E003",
          "error": "EMPTY_OBJECT",
          "message": "更新対象のフィールドがありません"
        }
        """
)
public class ErrorResponse {

  @Schema(description = "HTTPステータスコード",
      minimum = "400", maximum = "599",
      example = "400"
  )
  private int status;

  @Schema(description = "仕様化コード（例: E003, E001）",
      pattern = "^E\\d{3}$",
      example = "E003"
  )
  private String code;

  @Schema(description = "論理種別",
      // 必要に応じて増やしてください
      allowableValues = {
          "EMPTY_OBJECT",
          "MISSING_PARAMETER",
          "TYPE_MISMATCH",
          "INVALID_JSON",
          "INVALID_REQUEST",
          "VALIDATION_FAILED",
          "NOT_FOUND",
          "FORBIDDEN",
          "UNAUTHORIZED"
      },
      example = "EMPTY_OBJECT"
  )
  private String error;

  @Schema(
      description = "人間可読メッセージ（日本語）",
      example = "更新対象のフィールドがありません"
  )
  private String message;

  @ArraySchema(
      arraySchema = @Schema(
          description = "複数バリデーションエラー等（必要時のみ）",
          nullable = true
      ),
      schema = @Schema(implementation = FieldErrorDetail.class)
  )
  private List<FieldErrorDetail> errors;

  // 旧エイリアス。基本は出力しない（必要なケースのみ手動で設定）
  @ArraySchema(
      arraySchema = @Schema(
          description = "フィールド毎の詳細（必要時のみ / 互換用エイリアス）",
          nullable = true,
          deprecated = true
      ),
      schema = @Schema(implementation = FieldErrorDetail.class)
  )
  private List<FieldErrorDetail> details;

  // 便利ファクトリ（自動でdetailsにコピーしない → エイリアスは必要時のみ手動設定）
  public static ErrorResponse of(int status, String error, String code, String message) {
    return of(status, error, code, message, null, null);
  }

  public static ErrorResponse of(
      int status, String error, String code, String message,
      List<FieldErrorDetail> errors, List<FieldErrorDetail> details) {
    return new ErrorResponse(status, code, error, message, errors,
        details != null ? details : errors);
  }
}
