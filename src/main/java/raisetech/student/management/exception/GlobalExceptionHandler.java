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
 *
 * <p><strong>準拠事項（ErrorResponse 最終仕様）</strong></p>
 * <ul>
 *   <li>出力キーは <code>status</code>, <code>code</code>, <code>error</code>, <code>message</code> を基本とし、
 *       必要時のみ <code>errors</code>（複数バリデーション等）を付与します。</li>
 *   <li><em>レガシー別名</em>の <code>errorType</code> / <code>errorCode</code> は<strong>一切出力しません</strong>（互換目的でも不出力）。</li>
 *   <li><code>details</code> は互換用の別名として扱い、原則出力しません（やむを得ず出す場合でも
 *       <code>errors</code> と内容の重複を避けます）。</li>
 *   <li>原則として <code>code="E***"</code>、<code>error=種別ラベル</code>（例：EMPTY_OBJECT / MISSING_PARAMETER / TYPE_MISMATCH）に統一します。</li>
 * </ul>
 *
 * <p><strong>実装ノート</strong></p>
 * <ul>
 *   <li>ハンドラはすべて {@code build(...)} 系を経由し、キー順序や欠落キーを吸収して
 *       シリアライズの一貫性を保ちます。</li>
 *   <li>テスト（ErrorContract*）は、上記フォーマットへの準拠（レガシーキー不出力／必須キーの存在）を
 *       スモークとして常時検証します。</li>
 * </ul>
 *
 * <p>詳細仕様は README の「ErrorResponse（最終仕様）」を参照してください。</p>
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
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "E001", "入力値に不備があります",
        details);
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

  /// ========= 404: リソース未検出 =========
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
        ? ex.getMessage()
        : "指定されたリソースは見つかりませんでした";
    return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "E404", message, null);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
    return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "E404", "指定されたURLは存在しません", null);
  }

  /// ========= 400: JSON不正 / ボディ欠如 =========
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
    String raw = ex.getMessage();
    Throwable root = (ex.getCause() != null) ? ex.getCause() : ex;

    // 1) ボディ欠落（required=true で空 / 0byte / 途中終了 など）
    if (raw != null && (raw.contains("Required request body is missing")
        || raw.contains("No content to map")
        || raw.contains("Unexpected end-of-input")
        || raw.contains("JSON parse error")              // Spring 6 でよく見る
        || raw.contains("End-of-input")                  // Jackson 文言
    )) {
      return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E003",
          "リクエストボディは必須です。", null);
    }

    // 2) JSON 構造不正
    if (root instanceof com.fasterxml.jackson.core.JsonParseException
        || root instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException) {
      return build(HttpStatus.BAD_REQUEST, "INVALID_JSON", "E002",
          "リクエストのJSONが不正です。構造を確認してください。", null);
    }

    // 3) その他
    return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "E006",
        "リクエストボディの形式が不正です", null);
  }

  /// ========= 400: ID形式不正（Base64/UUID長） =========
  @ExceptionHandler(InvalidIdFormatException.class)
  public ResponseEntity<ErrorResponse> handleInvalidId(InvalidIdFormatException ex) {
    String msg = (ex.getMessage() != null && !ex.getMessage().isBlank())
        ? ex.getMessage()
        : "IDの形式が不正です";
    var details = List.of(new FieldErrorDetail("studentId", msg));
    return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "E006", msg, details);
  }

  /// ========= 400: 型不一致（クエリ/パスパラメータ変換） =========
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String field = ex.getName();
    String rejected = String.valueOf(ex.getValue());
    String expected =
        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";

    var detail = new FieldErrorDetail(
        field,
        String.format("型不一致（値='%s', 期待型=%s）", rejected, expected)
    );
    String msg = String.format("リクエストパラメータ '%s' の型が不正です", field);

    return build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", "E004", msg, List.of(detail));
  }

  /// ========= 400: 必須クエリ欠如 =========
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParam(
      MissingServletRequestParameterException ex) {
    String name = ex.getParameterName();
    String msg = String.format("必須パラメータ '%s' がありません", name);
    return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E003", msg, null);
  }

  /// ========= 400: バインディング不足/欠落（ヘッダ等含む） =========
  @ExceptionHandler(ServletRequestBindingException.class)
  public ResponseEntity<ErrorResponse> handleBinding(ServletRequestBindingException ex) {
    String message = "リクエストに必要なパラメータが不足しています: " + ex.getMessage();
    return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E003", message, null);
  }

  /// 　========= 400:空ボディ（Controller側で MissingParameterException を投げるケース）=========
  @ExceptionHandler(MissingParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameter(MissingParameterException ex) {
    String msg = (ex.getMessage() == null || ex.getMessage().isBlank())
        ? "リクエストボディは必須です。"
        : ex.getMessage();
    return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E003", msg, null);
  }

  /// ========= 400: 空オブジェクト（キーはあるが実質更新対象なし）=========
  // 用途: PATCH などで JSON ボディ自体はあるが、更新すべき値が 1つも無い場合に投げるアプリ独自例外。
  // 例: {} / {"student":{}} / {"appendCourses":null,"courses":[]} など、業務的に「更新なし」と判定。
  // 返却: 400 Bad Request / error=EMPTY_OBJECT / code=E003 / message=「更新対象のフィールドがありません」
  @ExceptionHandler(EmptyObjectException.class)
  public ResponseEntity<ErrorResponse> handleEmptyObject(EmptyObjectException ex) {
    String msg = (ex.getMessage() == null || ex.getMessage().isBlank())
        ? "更新対象のフィールドがありません"
        : ex.getMessage();
    return build(HttpStatus.BAD_REQUEST, "EMPTY_OBJECT", "E003", msg, null);
  }

  /// ========= 400: その他 IllegalArgumentException は E006 に寄せる =========
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleBadInput(IllegalArgumentException ex) {
    String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
        ? ex.getMessage()
        : "リクエスト形式が不正です";
    return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "E006", message, null);
  }

  /// ========= 403: 権限不足 =========
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "E403",
        "アクセスが拒否されました。管理者権限が必要です。", null);
  }

  /// ========= 500: 想定外 =========
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
  private ResponseEntity<ErrorResponse> build(
      HttpStatus status, String errorType, String errorCode, String message
  ) {
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
   * @param status    HTTP ステータス
   * @param errorType エラー種別ラベル（例: NOT_FOUND, VALIDATION_FAILED）
   * @param errorCode エラーコード（例: E404, E002）
   * @param message   メッセージ
   * @param details   フィールドエラー一覧（省略可）
   * @return エラーレスポンス
   */
  private ResponseEntity<ErrorResponse> build(
      HttpStatus status,
      String errorType,          // 例: VALIDATION_FAILED
      String errorCode,          // 例: E001
      String message,
      List<FieldErrorDetail> details // ない時は null を渡す
  ) {
    // （任意）引数チェックで誤用を早期発見
    if (isErrorCode(errorType) || !isErrorCode(errorCode)) {
      // テストや開発時は例外で即気づけるようにする（本番で例外にしたくなければ log.warn に変更）
      throw new IllegalArgumentException(
          "build(...) の引数順が不正です: errorType=" + errorType + ", errorCode=" + errorCode);
    }

    List<FieldErrorDetail> safe =
        (details == null || details.isEmpty()) ? null : List.copyOf(details);

    var body = ErrorResponse.builder()
        .status(status.value())
        .code(errorCode)     // ← E***
        .error(errorType)    // ← 種別ラベル
        .message(message)
        .errors(safe)
        .details(safe)       // エイリアス
        .build();

    return ResponseEntity.status(status).body(body);
  }

  // ヘルパー：Eddd 判定
  private static boolean isErrorCode(String s) {
    return s != null && s.matches("E\\d{3}");
  }
}
