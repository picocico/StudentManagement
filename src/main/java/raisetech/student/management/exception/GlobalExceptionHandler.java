package raisetech.student.management.exception;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import raisetech.student.management.exception.dto.ErrorResponse;
import raisetech.student.management.exception.dto.FieldErrorDetail;

/**
 * 例外→統一フォーマットの {@link ErrorResponse} へ変換するハンドラ群。
 *
 * <p><strong>準拠事項（ErrorResponse 最終仕様）</strong>
 *
 * <ul>
 *   <li>出力キーは <code>status</code>, <code>code</code>, <code>error</code>, <code>message</code>
 *       を基本とし、 必要時のみ <code>errors</code>（複数バリデーション等）を付与します。
 *   <li><em>レガシー別名</em>の <code>errorType</code> / <code>errorCode</code>
 *       は<strong>一切出力しません</strong>（互換目的でも不出力）。
 *   <li><code>details</code> は互換用の別名として扱い、原則出力しません（やむを得ず出す場合でも <code>errors</code> と内容の重複を避けます）。
 *   <li>原則として <code>code="E***"</code>、<code>error=種別ラベル</code>（例：EMPTY_OBJECT / MISSING_PARAMETER
 *       / TYPE_MISMATCH）に統一します。
 * </ul>
 *
 * <p><strong>実装ノート</strong>
 *
 * <ul>
 *   <li>ハンドラはすべて {@code build(...)} 系を経由し、キー順序や欠落キーを吸収して シリアライズの一貫性を保ちます。
 *   <li>テスト（ErrorContract*）は、上記フォーマットへの準拠（レガシーキー不出力／必須キーの存在）を スモークとして常時検証します。
 * </ul>
 *
 * <p>詳細仕様は README の「ErrorResponse（最終仕様）」を参照してください。
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

  // プライベートヘルパー： バリデーション結果をFieldErrorDetailリストに変換
  private static List<FieldErrorDetail> toFieldErrors(BindingResult br) {
    return br.getFieldErrors().stream()
        .map(err -> new FieldErrorDetail(err.getField(), err.getDefaultMessage()))
        .toList();
  }

  /// ========= 400: バリデーション失敗 (MethodArgumentNotValid) =========
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    var errors = toFieldErrors(ex.getBindingResult());
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "E001", "入力値に不備があります", errors);
  }

  /// ========= 400: バリデーション失敗 (ConstraintViolation) =========
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    var details =
        ex.getConstraintViolations().stream()
            .map(v -> new FieldErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
            .toList();
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "E001", "入力値に不備があります", details);
  }

  /// ========= 400: バリデーション失敗 (BindException) =========
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponse> handleBind(BindException ex) {
    var errors = toFieldErrors(ex.getBindingResult());
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "E001", "入力値に不備があります", errors);
  }

  /// ========= 400: JSON不正 / ボディ欠如 =========
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
    Throwable root = ex.getMostSpecificCause();
    // 1) JSON構文・マッピングのエラー → INVALID_JSON / E002
    if (root instanceof com.fasterxml.jackson.core.JsonParseException
        || root instanceof com.fasterxml.jackson.databind.JsonMappingException) {
      return build(HttpStatus.BAD_REQUEST, "INVALID_JSON", "E002", "JSONの構文が不正です。", null);
    }
    // 2) 空ボディや EOF っぽいケース → MISSING_PARAMETER / E001
    String msg = String.valueOf(ex.getMessage()).toLowerCase();
    if (msg.contains("no content")
        || msg.contains("required request body is missing")
        || root == null) {
      return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E001", "リクエストボディが必要です。", null);
    }
    // 3) それ以外はINVALID_REQUESTで吸収
    return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "E006", "不正なリクエストです。", null);
  }

  /// ========= 400: ID形式不正（Base64/UUID長） =========
  @ExceptionHandler(InvalidIdFormatException.class)
  public ResponseEntity<ErrorResponse> handleInvalidId(InvalidIdFormatException ex) {
    String msg =
        (ex.getMessage() != null && !ex.getMessage().isBlank()) ? ex.getMessage() : "IDの形式が不正です";
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

    var detail =
        new FieldErrorDetail(field, String.format("型不一致（値='%s', 期待型=%s）", rejected, expected));
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
    String msg =
        (ex.getMessage() == null || ex.getMessage().isBlank()) ? "リクエストボディは必須です。" : ex.getMessage();
    return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "E003", msg, null);
  }

  /// ========= 400: 空オブジェクト（キーはあるが実質更新対象なし）=========
  // 用途: PATCH などで JSON ボディ自体はあるが、更新すべき値が 1つも無い場合に投げるアプリ独自例外。
  // 例: {} / {"student":{}} / {"appendCourses":null,"courses":[]} など、業務的に「更新なし」と判定。
  // 返却: 400 Bad Request / error=EMPTY_OBJECT / code=E003 / message=「更新対象のフィールドがありません」
  @ExceptionHandler(EmptyObjectException.class)
  public ResponseEntity<ErrorResponse> handleEmptyObject(EmptyObjectException ex) {
    String msg =
        (ex.getMessage() == null || ex.getMessage().isBlank())
            ? "更新対象のフィールドがありません"
            : ex.getMessage();
    return build(HttpStatus.BAD_REQUEST, "EMPTY_OBJECT", "E003", msg, null);
  }

  /// ========= 400: その他 IllegalArgumentException は E006 に寄せる =========
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleBadInput(IllegalArgumentException ex) {
    String message =
        (ex.getMessage() != null && !ex.getMessage().isBlank()) ? ex.getMessage() : "リクエスト形式が不正です";
    return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "E006", message, null);
  }

  /// ========= 403: 権限不足 =========
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "E403", "アクセスが拒否されました。管理者権限が必要です。", null);
  }

  /// ========= 404: 存在しないURL (NoHandlerFound) =========
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
    return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "E404", "指定されたURLは存在しません", null);
  }

  /// ========= 404: リソース未検出 =========
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    String message =
        (ex.getMessage() != null && !ex.getMessage().isBlank())
            ? ex.getMessage()
            : "指定されたリソースは見つかりませんでした";
    return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "E404", message, null);
  }

  /// ========= 500: 想定外 =========
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "E999", "予期しないエラーが発生しました", null);
  }

  // ===== 共通ビルダ =====

  /**
   * 共通エラーレスポンスのビルダ（簡易版）。フィールドエラーが不要なケース向け。
   *
   * <p>内部的には詳細版に委譲します。引数の意味・順序は詳細版と同じです。
   *
   * @see #build(HttpStatus, String, String, String, java.util.List)
   */
  private ResponseEntity<ErrorResponse> build(
      HttpStatus status, String error, String code, String message) {
    return build(status, error, code, message, null, null);
  }

  /**
   * 共通エラーレスポンスのビルダ（エラー詳細リスト付き）。
   *
   * @see #build(HttpStatus, String, String, String, List, List)
   */
  private ResponseEntity<ErrorResponse> build(
      HttpStatus status, String error, String code, String message, List<FieldErrorDetail> errors) {
    return build(status, error, code, message, errors, null);
  }

  /**
   * 共通エラーレスポンスのビルダ（詳細版）。
   *
   * <p><b>呼び出し契約</b>： status … HTTPステータス（必須） / error … エラー種別ラベル（例: MISSING_PARAMETER,
   * VALIDATION_FAILED） / code … 仕様化コード（例: E001, E003, E404） / message … 人が読む説明文（空なら空文字に正規化） /
   * errors … フィールド単位の詳細（省略可。出力の主は errors） / details … 互換用エイリアス（原則非出力。errors が空のときのみ片方向コピー）
   *
   * <p><b>順序吸収ロジック</b>： error と code が取り違えられても E\\d{3} 判定で安全に補正します（例外は投げません）。
   *
   * <p><b>出力仕様</b>： body.error = error / body.code = code / body.errors のみ原則出力（details
   * は最終仕様では非推奨・通常非出力）
   *
   * <p><b>フォールバック</b>： ビルド中の予期せぬ例外や必須欠落時は error=INVALID_RESPONSE_BUILD, code=E999 で安全に応答します（決して
   * throw しません）。
   */
  private static final String FALLBACK_ERROR = "INVALID_RESPONSE_BUILD";

  private static final String FALLBACK_CODE = "E999";
  private static final String FALLBACK_MSG = "エラーレスポンスの生成に失敗しました。運用へ連絡してください。";

  private ResponseEntity<ErrorResponse> build(
      HttpStatus status,
      String error, // 論理種別
      String code, // 仕様化コード（E***）
      String message, // 人間可読
      List<FieldErrorDetail> errors, // optional
      List<FieldErrorDetail> details // optional（非推奨エイリアス）
      ) {
    try {
      String e = trim(error);
      String c = trim(code);
      String m = trim(message);

      // --- 順序吸収ロジック（“絶対に投げない”） --------------------------
      // 例: code と error が入れ替わっていたら補正する
      boolean looksLikeCode = (c != null && c.matches("E\\d{3}"));
      boolean looksLikeError = (e != null && !e.matches("E\\d{3}"));

      if (!looksLikeCode
          && e != null
          && e.matches("E\\d{3}")
          && (c == null || !c.matches("E\\d{3}"))) {
        // e=E*** / c=ラベル の取り違え
        log.warn("build(): arguments looked swapped. Swapping error/code. error={}, code={}", e, c);
        String tmp = e;
        e = c;
        c = tmp;
      }

      // 最低限のバリデーション（投げない）
      if (isBlank(e) || isBlank(c)) {
        log.warn(
            "build(): insufficient fields. error='{}', code='{}'. Fallback to safe payload.", e, c);
        return ResponseEntity.status(status)
            .body(
                ErrorResponse.of(
                    status.value(),
                    FALLBACK_ERROR,
                    FALLBACK_CODE,
                    nonBlankOrDefault(m, FALLBACK_MSG),
                    null,
                    null));
      }

      // details は互換のため“読み出し専用 alias”。出力は『errors が主』に統一
      // 呼び出し側が details を直指定してきた場合は、errors が空のときのみ片方向コピー
      // errors 側だけを最終出力として採用する
      List<FieldErrorDetail> outErrors = errors;
      if ((outErrors == null || outErrors.isEmpty()) && details != null && !details.isEmpty()) {
        outErrors = details; // 互換入力→errors へ片方向コピー
      }

      return ResponseEntity.status(status)
          .body(
              ErrorResponse.of(
                  status.value(),
                  e, // error
                  c, // code
                  nonBlankOrDefault(m, ""), // message
                  outErrors,
                  null // details は最終仕様で常に非出力
                  ));

    } catch (Exception ex) {
      // ★ 例外ハンドラ内では“絶対に投げない”。ログだけ出してフォールバックを返す
      log.error("build(): unexpected exception. Fallback response returned.", ex);
      return ResponseEntity.status(status)
          .body(
              ErrorResponse.of(
                  status.value(), FALLBACK_ERROR, FALLBACK_CODE, FALLBACK_MSG, null, null));
    }
  }

  // NOTE: テスト用アダプタ。将来的に削除してよい。
  // 逆順渡しを含むビルド挙動をユニットテストから直接検証するための入口。
  static ResponseEntity<ErrorResponse> buildForTest(
      HttpStatus status, String error, String code, String message) {
    return new GlobalExceptionHandler() // インスタンス生成でもOK（state無いので安全）
        .build(status, error, code, message, null, null);
  }

  private static String trim(String s) {
    return s == null ? null : s.trim();
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String nonBlankOrDefault(String s, String def) {
    return isBlank(s) ? def : s;
  }

  // ヘルパー：Eddd 判定
  private static boolean isErrorCode(String s) {
    return s != null && s.matches("E\\d{3}");
  }
}
