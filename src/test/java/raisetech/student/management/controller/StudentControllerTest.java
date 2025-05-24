package raisetech.student.management.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import raisetech.student.management.service.StudentService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@link StudentController} のバリデーションエラーハンドリングを検証する単体テストクラス。
 * <p>
 * Spring MVC の {@code MockMvc} を使用して、フォーム送信に対するコントローラの動作を検証します。
 */
@ExtendWith(MockitoExtension.class)
public class StudentControllerTest {

  /**
   * 疑似的なHTTPリクエスト・レスポンスを再現するテスト用オブジェクト。
   */
  private MockMvc mockMvc;

  /**
   * モック化された {@link StudentService}。
   * <p>
   * コントローラの依存性として注入され、テスト対象外のサービスロジックを切り離します。
   */
  @Mock
  private StudentService service;

  /**
   * テスト対象となる {@link StudentController}。
   * <p>
   * モック化された依存を持つ状態でテスト実行されます。
   */
  @InjectMocks
  private StudentController controller;

  /**
   * 各テスト実行前に {@code MockMvc} を初期化します。
   */
  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  /**
   * バリデーションエラーを含む受講生更新リクエストに対し、
   * 適切なビュー名・ステータス・モデル属性が返されることを検証します。
   * <p>
   * 主な検証内容：
   * <ul>
   *   <li>バリデーション失敗時もステータスは200 OKで返ること</li>
   *   <li>ビュー名が "editStudent" であること</li>
   *   <li>エラーメッセージを含むモデル属性 "validationErrors" が存在すること</li>
   * </ul>
   *
   * @throws Exception HTTP通信の模擬処理中に例外が発生した場合
   */
  @Test
  public void testUpdateStudentWithValidationErrors() throws Exception {

    String studentId = "dadaf78f-c8c9-4725-a0a6-ed3bf31dd215";

    mockMvc.perform(post("/updateStudent")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("student.studentId", studentId)   // studentIdはstudentオブジェクトのフィールドとして送信
            .param("student.fullName", "")  // エラーを意図的に発生させる
            .param("student.furigana", "")  // エラーを意図的に発生させる
            .param("student.nickname", "")  // エラーを意図的に発生させる
            .param("student.email", "invalid-email")  // エラーを意図的に発生させる
            .param("student.gender", "")  // エラーを意図的に発生させる
            .accept(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isOk())  // エラーが発生しても200 OKが返る
        .andExpect(model().attributeExists("validationErrors"))  // エラーオブジェクトが存在するか
        .andExpect(view().name("editStudent"));  // エラー時に表示されるページ名
  }
}
















