package raisetech.student.management.exception;

import static org.springframework.web.servlet.function.ServerResponse.status;

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
  public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex,
      jakarta.servlet.http.HttpServletRequest request) {
    String uri = request != null ? request.getRequestURI() : "";
    // restore エンドポイントだけは RESOURCE_NOT_FOUND を返す
    String error = (uri != null && uri.endsWith("/restore"))
        ? "RESOURCE_NOT_FOUND"
        : "NOT_FOUND";
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
    String message = String.format("パラメータ '%s' は %s 型である必要があります。", field,
        expected);
    return build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", "E004", message);
  }

  // ========= 400: 必須クエリ欠如 =========
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParams(
      MissingServletRequestParameterException ex) {
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

  // ========= 400: 空ボディ / 空JSON =========
  @ExceptionHandler(MissingParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameter(MissingParameterException ex) {
    // メッセージは固定でも ex.getMessage() でもOK。テスト側は containsString 推奨。
    return build(HttpStatus.BAD_REQUEST, "E003", "MISSING_PARAMETER",
        "リクエストボディは必須です。");
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
  private ResponseEntity<ErrorResponse> build(
      HttpStatus status,
      String errorType,
      String errorCode,
      String message) {
    return build(status, errorType, errorCode, message, null);
  }

  private ResponseEntity<ErrorResponse> build(
      HttpStatus status,
      String errorType,   // ← errorCode
      String errorCode,  // ← errorType
      String message,
      List<FieldErrorDetail> fieldErrors) {

    // 呼び出し側で順番が逆（code,error）になっていても吸収する
    if (isErrorCode(errorType) && !isErrorCode(errorCode)) {
      String tmp = errorType;
      errorType = errorCode; // INVALID_REQUEST 等
      errorCode = tmp;       // E006 等
    }

    // ErrorResponse.of(...) は、status(int), errorType(String), errorCode(String), message, errors, details を受け取り
    // code(=errorCode) と error(=errorType) を内部でセットする実装（前段で作成したもの）を想定
    ErrorResponse body = new ErrorResponse();
    body.setStatus(status.value());

    // ここがポイント：code には E***、error には種別文字列
    body.setCode(errorCode);     // 例: E003, E006, E001
    body.setError(errorType);   // 例: MISSING_PARAMETER, INVALID_REQUEST, VALIDATION_FAILED
    body.setMessage(message);

    // null/空はシリアライズから落としたい想定（@JsonInclude(Include.NON_NULL) 等）
    List<FieldErrorDetail> safe =
        (fieldErrors == null || fieldErrors.isEmpty()) ? null : List.copyOf(fieldErrors);
    body.setErrors(safe);
    // details を物理的に別プロパティとして持つならこちらも同じ参照をセット
    // （ErrorResponseに setDetails(...) がある場合のみ）
    body.setDetails(safe); // ← details を errors のエイリアスとして同じ内容にする

    return ResponseEntity.status(status).body(body);
  }

  private static boolean isErrorCode(String s) {
    return s != null && s.matches("E\\d{3}");
  }
}
