package raisetech.student.management.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * {@link GlobalExceptionHandler} のテストクラス。
 *
 * <p>このクラスは、{@link GlobalExceptionHandler#buildForTest(HttpStatus, String, String, String)} メソッドの
 * 引数（エラー概要とエラーコード）の順序が入れ替わった場合の自動補正機能をテストします。
 */
class GlobalExceptionHandlerSwapOrderTest {

  /**
   * {@code buildForTest} メソッドに渡すエラー概要（error）とエラーコード（code）の引数を あえて逆にして呼び出した場合に、メソッド内部でそれらが自動的に補正され、
   * 期待通りの {@link raisetech.student.management.exception.dto.ErrorResponse} が 生成されることを検証します。
   *
   * <p><b>テストケース：</b>
   *
   * <ul>
   *   <li>引数(code)にエラー概要（例: "EMPTY_OBJECT"）を渡す
   *   <li>引数(error)にエラーコード（例: "E003"）を渡す
   * </ul>
   *
   * <b>期待する結果：</b>
   *
   * <ul>
   *   <li>レスポンスボディの {@code error} フィールドには "EMPTY_OBJECT" が設定される
   *   <li>レスポンスボディの {@code code} フィールドには "E003" が設定される
   * </ul>
   */
  @Test
  void build_swapped_error_and_code_are_auto_corrected() {
    var res =
        GlobalExceptionHandler.buildForTest(
            HttpStatus.BAD_REQUEST,
            "E003", // 本来は code。わざと逆に渡す
            "EMPTY_OBJECT", // 本来は error。わざと逆に渡す
            "dummy");

    // HTTPステータスの検証
    assertEquals(400, res.getStatusCode().value());
    var body = res.getBody();
    assertNotNull(body);

    // 引数が入れ替わっていても、自動補正されて正しいフィールドに格納されることを検証
    assertEquals("EMPTY_OBJECT", body.getError()); // ← 補正されている
    assertEquals("E003", body.getCode()); // ← 補正されている
  }
}
