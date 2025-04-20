package raisetech.student.management.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * アプリケーション全体で発生する例外を一元的に処理するハンドラー。
 * <p>
 * 各種カスタム例外に対応し、適切なHTTPステータスコードとエラーメッセージを返却します。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

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
}
