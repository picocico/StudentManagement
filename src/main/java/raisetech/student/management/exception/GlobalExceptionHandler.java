package raisetech.student.management.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import raisetech.student.management.exception.dto.ErrorResponse;
import raisetech.student.management.exception.dto.FieldErrorDetail;

/**
 * 例外→統一フォーマットの {@link ErrorResponse} へ変換するハンドラ群。
 * <p>原則として {@code code=E***}, {@code error=種別ラベル} の形に統一します。
 * 詳細エラーがある場合は {@code build(..., fieldErrors)} を利用します。</p>
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

  /// ========= 400: バリデーション失敗 =========
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    var details = ex.getBindingResult().getFieldErrors().stream()
        .map(err -> new FieldErrorDetail(err.getField(), err.getDefaultMessage()))
        .toList();
    // ★ ここをテスト期待に合わせる
    String msg = "入力値に不備があります";
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "E001", msg, details);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    var details = ex.getConstraintViolations().stream()
        .map(v -> new FieldErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
        .toList();
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "E001", "入力値に不備があります",
        details);
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponse> handleBind(BindException ex) {
    var details = ex.getBindingResult().getFieldErrors().stream()
        .map(err -> new FieldErrorDetail(err.getField(), err.getDefaultMessage()))
        .toList();
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "E001", "入力値に不備があります",
        details);
  }

  // ========= 404: リソース未検出 =========
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
        ? ex.getMessage()
        : "指定されたリソースは見つかりませんでした";
    return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "E404", message, null);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
    return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "E404", "指定されたURLは存在しません");
  }

  // ========= 400: JSON不正 / ボディ欠如 =========
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
    String raw = ex.getMessage();

    // 1) ボディ欠落
    if (raw != null && (
        raw.contains("Required request body is missing")
            || raw.contains("No content to map")
            || raw.contains("Unexpected end-of-input"))) {
      return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E003",
          "リクエストボディは必須です。");
    }

    // 2) JSON不正（Jacksonの代表的な原因も拾う）
    Throwable root = ex.getCause();
    if (root instanceof com.fasterxml.jackson.core.JsonParseException
        || root instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException) {
      return build(HttpStatus.BAD_REQUEST, "INVALID_JSON", "E002",
          "リクエストのJSONが不正です。構造を確認してください。");
    }

    // 3) その他もまとめて 400 / E006
    return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "E006",
        "リクエストボディの形式が不正です");
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
    String name = ex.getName();                 // 例: includeDeleted / deletedOnly
    Object value = ex.getValue();               // 例: abc / xyz
    String required =
        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
    String msg = String.format("リクエストパラメータ '%s' の型が不正です（値='%s'、期待型=%s）", name,
        value, required);
    return build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", "E004", msg);
  }

  // ========= 400: 必須クエリ欠如 =========
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParam(
      MissingServletRequestParameterException ex) {
    String name = ex.getParameterName();
    String msg = String.format("必須パラメータ '%s' がありません", name);
    return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E003", msg, null);
  }

  // ========= 400: バインディング不足/欠落（ヘッダ等含む） =========
  @ExceptionHandler(ServletRequestBindingException.class)
  public ResponseEntity<ErrorResponse> handleBinding(ServletRequestBindingException ex) {
    String message = "リクエストに必要なパラメータが不足しています: " + ex.getMessage();
    return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E003", message);
  }

  // ========= 400: 空オブジェクト =========
  @ExceptionHandler(EmptyObjectException.class)
  public ResponseEntity<ErrorResponse> handleEmptyObject(EmptyObjectException ex) {
    return build(HttpStatus.BAD_REQUEST, "EMPTY_OBJECT", "E003",
        ex.getMessage() != null ? ex.getMessage() : "更新対象のフィールドがありません");
  }

  // ========= 400: 空ボディ / 空JSON（独自） =========
  @ExceptionHandler(MissingParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameter(MissingParameterException ex) {
    return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E003",
        ex.getMessage() != null ? ex.getMessage() : "リクエストボディは必須です。");
  }

  // ========= 400: その他 IllegalArgumentException は E006 に寄せる =========
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleBadInput(IllegalArgumentException ex) {
    String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
        ? ex.getMessage()
        : "リクエスト形式が不正です";
    return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "E006", message, null);
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
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "E999",
        "予期しないエラーが発生しました", null);
  }

  // ===== 共通ビルダ =====

  /**
   * 共通エラーレスポンスのビルダ（簡易版）。フィールドエラーが不要なケース向け。
   *
   * <p>内部的には詳細版に委譲します。引数の意味・順序は詳細版と同じです。</p>
   *
   * @see #build(HttpStatus, String, String, String, java.util.List)
   */
  private ResponseEntity<ErrorResponse> build(HttpStatus status, String errorType, String errorCode,
      String message) {
    return build(status, errorType, errorCode, message, null);
  }

  /**
   * 共通エラーレスポンスのビルダ（詳細版）。
   *
   * <p><b>呼び出し契約</b>：
   * <ul>
   *   <li><code>status</code> … HTTP ステータス（必須）</li>
   *   <li><code>errorType</code> … エラー種別の表記（例: {@code MISSING_PARAMETER}, {@code VALIDATION_FAILED}）。</li>
   *   <li><code>errorCode</code> … コード（例: {@code E001}, {@code E003}, {@code E404}）。</li>
   *   <li><code>message</code> … 人が読む説明文。</li>
   *   <li><code>fieldErrors</code> … フィールド単位のエラー詳細。{@code null} または空の場合、レスポンスの
   *       {@code errors}/{@code details} は省略（非出力）されます。</li>
   * </ul>
   *
   * <p><b>順序吸収ロジック</b>：
   * 呼び出し側が誤って <code>errorType</code> と <code>errorCode</code> を逆に渡しても、
   * 本メソッド内で {@code E\\d{3}} 形式の判定により自動で補正します。
   * したがって、通常は <code>build(status, "VALIDATION_FAILED", "E002", ...)</code> の順で
   * 渡すことを推奨します。</p>
   *
   * <p><b>出力</b>：
   * <ul>
   *   <li>{@code body.code} には <code>errorCode</code>（例: {@code E002}）が入ります。</li>
   *   <li>{@code body.error} には <code>errorType</code>（例: {@code VALIDATION_FAILED}）が入ります。</li>
   *   <li>{@code body.errors} と {@code body.details} は同内容（エイリアス）になります。</li>
   * </ul>
   *
   * @param status      HTTP ステータス
   * @param errorType   エラー種別ラベル（例: NOT_FOUND, VALIDATION_FAILED）
   * @param errorCode   エラーコード（例: E404, E002）
   * @param message     メッセージ
   * @param fieldErrors フィールドエラー一覧（省略可）
   * @return エラーレスポンス
   */
  private ResponseEntity<ErrorResponse> build(
      HttpStatus status,
      String errorType,   // ← errorCode
      String errorCode,   // ← errorType
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
    body.setDetails(safe);// ← details を errors のエイリアスとして同じ内容にする
    // details を物理的に別プロパティとして持つならこちらも同じ参照をセット
    // （ErrorResponseに setDetails(...) がある場合のみ）
    return ResponseEntity.status(status).body(body);
  }

  private static boolean isErrorCode(String s) {
    return s != null && s.matches("E\\d{3}");
  }
}
