package raisetech.student.management.exception;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.security.access.AccessDeniedException;

import raisetech.student.management.exception.dto.ErrorResponse;
import raisetech.student.management.exception.dto.FieldErrorDetail;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // ========= 400: バリデーション失敗 =========
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .map(err -> new FieldErrorDetail(err.getField(), err.getDefaultMessage()))
        .collect(Collectors.toList());
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "E001",
        "入力値に不備があります", fieldErrors);
  }

  // ========= 404: リソース未検出 =========
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
    // テスト互換の軽量JSON（順序安定のため LinkedHashMap）
    Map<String, Object> body = new java.util.LinkedHashMap<>();
    body.put("status", HttpStatus.NOT_FOUND.value());
    body.put("code", "E404");                 // 文字列で返す
    body.put("error", "RESOURCE_NOT_FOUND");  // ★ テスト期待
    body.put("message", ex.getMessage());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
    return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "E404",
        "指定されたURLは存在しません");
  }

  // ========= 400: JSON不正 / ボディ欠如 =========
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex) {
    String msg = ex.getMessage();
    boolean missingBody = msg != null && (
        msg.contains("Required request body is missing")
            || msg.contains("No content to map")
            || msg.contains("Unexpected end-of-input"));
    if (missingBody) {
      return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E003",
          "リクエストボディは必須です。");
    }
    return build(HttpStatus.BAD_REQUEST, "INVALID_JSON", "E002",
        "リクエストの形式が不正です。JSON構造を確認してください。");
  }

  // ========= 400: ID形式不正（独自） =========
  @ExceptionHandler(InvalidIdFormatException.class)
  public ResponseEntity<ErrorResponse> handleInvalidId(InvalidIdFormatException ex) {
    return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "E006",
        ex.getMessage() != null ? ex.getMessage() : "IDの形式が不正です");
  }

  // ========= 400: 型不一致（パラメータ変換） =========
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String field = ex.getName();
    String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "不明";
    String message = String.format("パラメータ '%s' は %s 型である必要があります。", field, expected);
    return build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", "E004", message);
  }

  // ========= 400: 必須クエリ欠如 =========
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex) {
    String message = String.format("リクエストパラメータ '%s' は必須です。", ex.getParameterName());
    return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E003", message);
  }

  // ========= 400: バインディング失敗 =========
  @ExceptionHandler(ServletRequestBindingException.class)
  public ResponseEntity<ErrorResponse> handleBinding(ServletRequestBindingException ex) {
    String message = "リクエストに必要なパラメータが不足しています: " + ex.getMessage();
    return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E005", message);
  }

  // ========= 400: 空オブジェクト =========
  @ExceptionHandler(EmptyObjectException.class)
  public ResponseEntity<ErrorResponse> handleEmptyObject(EmptyObjectException ex) {
    return build(HttpStatus.BAD_REQUEST, "EMPTY_OBJECT", "E003",
        ex.getMessage() != null ? ex.getMessage() : "更新対象のフィールドがありません");
  }

  // ========= 400: BindException（フォーム系） =========
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponse> handleBind(BindException ex) {
    List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .map(err -> new FieldErrorDetail(err.getField(), err.getDefaultMessage()))
        .collect(Collectors.toList());
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "E001",
        "入力値が不正です。", fieldErrors);
  }

  // ========= 400: その他 IllegalArgumentException は E006 に寄せる =========
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    log.error("Unhandled IllegalArgumentException", ex);
    return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "E006",
        ex.getMessage() != null ? ex.getMessage() : "無効なリクエストです");
  }

  // ========= 403: 権限不足 =========
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "E403",
        "アクセスが拒否されました。管理者権限が必要です。");
  }

  // ========= 500: 想定外 =========
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
    log.error("Unhandled exception occurred", ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "E999",
        "予期しないエラーが発生しました");
  }

  // ===== 共通ビルダ =====
  private ResponseEntity<ErrorResponse> build(HttpStatus status,
      String errorType,
      String errorCode,
      String message) {
    return build(status, errorType, errorCode, message, null);
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus status,
      String errorType,
      String errorCode,
      String message,
      List<FieldErrorDetail> fieldErrors) {
    // ErrorResponse.of(...) は、status(int), errorType(String), errorCode(String), message, errors, details を受け取り
    // code(=errorCode) と error(=errorType) を内部でセットする実装（前段で作成したもの）を想定
    ErrorResponse body = ErrorResponse.of(
        status.value(),          // status:int
        errorType,               // error / errorType
        errorCode,               // code / errorCode (E###)
        message,
        fieldErrors,
        fieldErrors              // details は errors のエイリアス
    );
    return ResponseEntity.status(status).body(body);
  }
}
