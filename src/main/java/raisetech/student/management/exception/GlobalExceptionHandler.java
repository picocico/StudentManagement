package raisetech.student.management.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.ServletRequestBindingException;
import raisetech.student.management.exception.dto.ErrorResponse;
import raisetech.student.management.exception.dto.FieldErrorDetail;
import org.springframework.security.access.AccessDeniedException;


/**
 * アプリケーション全体で発生する例外を一元的に処理するハンドラー。
 * <p>
 * 各種カスタム例外に対応し、適切なHTTPステータスコードとエラーメッセージを返却します。
 */
@RestControllerAdvice(basePackages = "raisetech.student.management.controller")
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * バリデーション失敗時に呼び出される例外ハンドラー。
   * <p>
   * 入力エラーの内容をフィールドごとに収集し、統一形式のエラーレスポンスを返却します。
   *
   * @param ex バリデーション例外（MethodArgumentNotValidException）
   * @return 入力エラーの詳細を含む400 Bad Requestレスポンス
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
    List<FieldErrorDetail> errorDetails = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
        .collect(Collectors.toList());

    ErrorResponse response = new ErrorResponse(
        400,                             // HTTPステータス
        HttpStatus.BAD_REQUEST.value(),  // 整数のステータスコード（
        "VALIDATION_FAILED",             // エラータイプ（固定文字列）
        "E001",                          // 独自のエラーコード
        "入力値に不備があります",           // エラーメッセージ
        errorDetails                    // フィールドエラー
    );

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * 指定されたリソースが見つからない場合に呼び出される例外ハンドラー。
   * <p>
   * エラーメッセージ付きの404 Not Foundレスポンスを返します。
   *
   * @param ex リソース未検出時にスローされる例外（ResourceNotFoundException）
   * @return 404ステータスとエラーメッセージを含むレスポンス
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
    ErrorResponse response = new ErrorResponse(
        404,
        HttpStatus.NOT_FOUND.value(),
        "NOT_FOUND",
        "E404",
        ex.getMessage(),
        null
    );

    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
  }

  /**
   * リクエストに無効な引数が含まれていた場合の例外処理。
   * <p>
   * 例：Base64形式のIDが正しくデコードできなかった場合などに使用されます。
   *
   * @param ex 不正な引数に起因する例外（IllegalArgumentException）
   * @return 400 Bad Request ステータスと説明メッセージを含むレスポンス
   */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    logger.error("Unhandled exception occurred: {}", ex.getClass().getName(), ex);
    String message = ex.getMessage() != null ? ex.getMessage() : "無効なリクエストです。";

    // Base64関連の例外は型不一致として分類
    if (message.contains("Last unit does not have enough valid bits")
        || message.contains("Illegal base64 character")
        || message.contains("Illegal base64")) {
    ErrorResponse response = new ErrorResponse(
        400,
        HttpStatus.BAD_REQUEST.value(),
        "TYPE_MISMATCH",
        "E004",
        "IDの形式が不正です（Base64）",
        null
    );

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

    // それ以外は通常の不正リクエスト扱い
    ErrorResponse response = new ErrorResponse(
        400,
        HttpStatus.BAD_REQUEST.value(),
        "INVALID_REQUEST",
        "E006",
        "無効なリクエストです: " + message,
        null
    );
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * リクエストボディのJSON形式が不正な場合の例外処理。
   *
   * @param ex HttpMessageNotReadableException
   * @return 400 Bad Requestレスポンス
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex) {
    ErrorResponse response = new ErrorResponse(
        400,
        HttpStatus.BAD_REQUEST.value(),
        "INVALID_JSON",
        "E002",
        "リクエストの形式が不正です。JSON構造を確認してください。",
        null
    );
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * クエリパラメータやパス変数の型不一致に関する例外処理。
   *
   * @param ex MethodArgumentTypeMismatchException
   * @return 400 Bad Requestレスポンス
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String field = ex.getName();
    String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "不明";
    String message = String.format("パラメータ '%s' は %s 型である必要があります。", field, expectedType);

    ErrorResponse response = new ErrorResponse(
        400,
        HttpStatus.BAD_REQUEST.value(),
        "TYPE_MISMATCH",
        "E004",
        message,
        null
    );

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * 必須リクエストパラメータが不足している場合の例外処理。
   *
   * @param ex MissingServletRequestParameterException
   * @return 400 Bad Requestレスポンス
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex) {
    String message = String.format("リクエストパラメータ '%s' は必須です。", ex.getParameterName());

    ErrorResponse response = new ErrorResponse(
        400,
        HttpStatus.BAD_REQUEST.value(),
        "MISSING_PARAMETER",
        "E003",
        message,
        null
    );

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * リクエストのバインディングに失敗した場合の処理。
   *
   * @param ex ServletRequestBindingException
   * @return 400 Bad Requestレスポンス
   */
  @ExceptionHandler(ServletRequestBindingException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleBinding(ServletRequestBindingException ex) {
    String message = String.format("リクエストに必要なパラメータが不足しています: %s", ex.getMessage());

    ErrorResponse response = new ErrorResponse(
        400,
        HttpStatus.BAD_REQUEST.value(),
        "MISSING_PARAMETER",
        "E005",
        message,
        null
    );

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * 権限不足によるアクセス拒否（403 Forbidden）時に呼び出される例外ハンドラー。
   * <p>
   * 認証は成功しているが、必要なロールや権限が不足している場合に発生します。
   * REST APIとして統一されたエラーレスポンスを返却します。
   *
   * @param ex Spring Security によってスローされる AccessDeniedException
   * @return アクセス拒否のエラー内容を含む 403 Forbidden レスポンス
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
    ErrorResponse response = new ErrorResponse(
        403,
        HttpStatus.FORBIDDEN.value(),
        "FORBIDDEN",
        "E403",
        "アクセスが拒否されました。管理者権限が必要です。",
        null
    );

    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
  }

  /**
   * ハンドリングされていない予期せぬ例外の処理。
   * <p>
   * システム内部の問題など、特定のハンドラーで処理されない例外を捕捉し、
   * 500 Internal Server Error を返します。
   *
   * @param ex 捕捉されなかった一般的な例外（Exception）
   * @return 500 Internal Server Error ステータスと汎用的なエラーメッセージを含むレスポンス
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
    logger.error("Unhandled exception occurred", ex);
    ErrorResponse response = new ErrorResponse(
        500,
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "INTERNAL_ERROR",
        "E999",
        "予期しないエラーが発生しました",
        null
    );

    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
