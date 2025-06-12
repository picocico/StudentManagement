package raisetech.student.management.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import raisetech.student.management.config.TestMockConfig;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.exception.GlobalExceptionHandler;
import raisetech.student.management.service.StudentService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@link StudentController} の例外ハンドリングを検証するテストクラス。
 * <p>
 * REST APIの例外動作をSpring MVC の {@code MockMvc} を使用して確認します。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestMockConfig.class, GlobalExceptionHandler.class})
public class StudentControllerTest {

  /**
   * 疑似的なHTTPリクエスト・レスポンスを再現するテスト用オブジェクト。
   */
  @Autowired
  private MockMvc mockMvc;

  /**
   * モック化された {@link StudentService}。
   * <p>
   * コントローラの依存性として注入され、テスト対象外のサービスロジックを切り離します。
   */
  @Autowired
  private StudentService service;

  /**
   * テスト対象の {@link StudentController} に注入されるモックの StudentConverter。
   * <p>
   * 実際のエンティティ・DTO変換処理は行わず、必要に応じて動作をスタブ化することで
   * コントローラーの単体テストを実現します。
   */
  @Autowired
  private StudentConverter converter;

  /**
   * 各テスト実行前に {@code MockMvc} を初期化します。
   */
  @BeforeEach
  public void setup() {
    // 必要に応じて stub 設定もここで追加できます
  }

  /**
   * バリデーションエラーがある状態で受講生を登録しようとしたときに、
   * 適切なHTTPステータスとエラーレスポンスが返却されることを検証します。
   * <p>
   * 主な検証内容：
   * <ul>
   *   <li>ステータスコードが 400 Bad Request であること</li>
   *   <li>レスポンスボディに errorCode が "E001" として含まれること</li>
   *   <li>エラー詳細（errors）が配列として含まれていること</li>
   * </ul>
   *
   * @throws Exception HTTP通信の模擬処理中に例外が発生した場合
   */
  @Test
  public void testRegisterStudentWithValidationErrors() throws Exception {
    String invalidRequestJson = """
      {
        "student": {
          "fullName": "",
          "furigana": "",
          "nickname": "",
          "email": "invalid-email",
          "location": "",
          "age": -1,
          "gender": "",
          "remarks": ""
        },
        "courses": []
      }
      """;

    mockMvc.perform(post("/api/students")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequestJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("400"))
        .andExpect(jsonPath("$.errorCode").value("E001"))  // バリデーションエラーコード
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThan(0)));
  }

  /**
   * MissingServletRequestParameterException のテスト。
   * 'keyword' パラメータなしで呼び出して 400 を期待。
   */
  @Test
  public void testMissingServletRequestParameterException() throws Exception {
    mockMvc.perform(get("/api/students/test-missing-param")) // keyword を指定しない
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("E003")); // GlobalExceptionHandler 側のエラーコードに応じて変更
  }

  /**
   * MethodArgumentTypeMismatchException のテスト。
   * 'id' に不正な型（例: abc）を指定して 400 を期待。
   */
  @Test
  public void testMethodArgumentTypeMismatchException() throws Exception {
    mockMvc.perform(get("/api/students/test-type")
            .param("id", "abc")) // int 型に変換できない文字列
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("E004"));
  }
}

















