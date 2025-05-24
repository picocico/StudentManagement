package raisetech.student.management.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * アプリケーション全体で発生する例外を一元的に処理するハンドラー。
 * <p>
 * 各種カスタム例外に対応し、適切なHTTPステータスコードとエラーメッセージを返却します。
 */
@ControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * バリデーション失敗時の例外処理。
   *
   * @param ex バリデーション例外
   * @return エラーメッセージ付きのレスポンス
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error ->
        errors.put(error.getField(), error.getDefaultMessage()));

    Map<String, Object> responseBody = new HashMap<>();
    responseBody.put("status", "error");
    responseBody.put("validationErrors", errors);
    return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
  }

  /**
   * リソースが見つからなかった場合の例外処理。
   *
   * @param ex リソース未検出例外
   * @return メッセージ付きの404レスポンス
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
    Map<String, Object> responseBody = new HashMap<>();
    responseBody.put("status", "error");
    responseBody.put("message", ex.getMessage());
    return new ResponseEntity<>(responseBody, HttpStatus.NOT_FOUND);
  }

  /**
   * リクエストが不正（例：IDのデコード失敗など）なときの処理。
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    Map<String, Object> responseBody = new HashMap<>();
    responseBody.put("status", "error");
    responseBody.put("message", "無効なリクエストです: " + ex.getMessage());
    return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
  }

  /**
   * その他の予期せぬ例外。
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleGeneralException(Exception ex) {
    logger.error("Unhandled exception occurred", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("予期しないエラーが発生しました。");
  }
}
