package raisetech.student.management.exception.dto;

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
public class ErrorResponse {

  private int status;       // "error"
  private int code;            // HTTPステータスコード
  private String errorType;    // アプリケーション分類
  private String errorCode;    // アプリケーション固有コード
  private String message;      // ユーザー向けメッセージ
  private List<FieldErrorDetail> errors; // フィールド単位のエラー（省略可）

}
