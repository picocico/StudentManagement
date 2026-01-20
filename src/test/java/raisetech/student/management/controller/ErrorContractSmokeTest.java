package raisetech.student.management.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import raisetech.student.management.config.TestSecurityConfig;
import raisetech.student.management.data.Student;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.exception.GlobalExceptionHandler;
import raisetech.student.management.web.RawBodyCaptureFilter;

/**
 * エラーレスポンスのJSONスキーマ（契約）が安定していることを確認するスモークテストクラス。
 *
 * <p>{@link GlobalExceptionHandler} によって生成されるエラーレスポンスが、 意図したキー（code, error,
 * message）のみを含み、レガシーなキー（errorCode, errorType）や 状況によって存在しないキー（details,
 * errors）を正しくハンドリングしていることを検証します。
 *
 * @see ControllerTestBase
 */
@WebMvcTest(controllers = StudentController.class)
@ActiveProfiles("test")
// 403/401 を避けるテストではテスト用Securityを有効化
@Import({TestSecurityConfig.class, RawBodyCaptureFilter.class})
@AutoConfigureMockMvc(addFilters = true) // フィルタを有効化
class ErrorContractSmokeTest extends ControllerTestBase {

  /**
   * 各スモークテストの実行前に、共通のスタブを設定します。
   *
   * <p>ベースクラスの reset の後、正常系のスタブを注入し、 404エラー（NotFound）が発生しないように サービス層のスタブ（空のStudent）も設定します。
   */
  @BeforeEach
  void setUpSmoke() {
    // ベースの @BeforeEach で reset 済みなので、共通スタブを流し込む
    stubConverterHappyPath();
    stubServiceHappyPath();

    // 念のため：NotFound させない
    when(service.findStudentById(any())).thenReturn(new Student());
  }

  /**
   * 型不一致（TypeMismatchException）による 400 Bad Request レスポンスのスキーマを検証します。
   *
   * <p>このテストでは、レスポンスに 'errors' または 'details' のどちらかの配列が
   * 少なくとも一つ含まれていることを柔軟にチェックします。（Springのバージョン等による差異を許容するため）
   *
   * @throws Exception MockMvc実行時例外
   */
  @Test
  void 型不一致時_エラーレスポンスのスキーマが安定していること() throws Exception {
    mockMvc
        .perform(get("/api/students").param("includeDeleted", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").isNumber())
        .andExpect(jsonPath("$.code").isString())
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        // ここを柔軟に：
        .andExpect(
            result -> {
              var json = result.getResponse().getContentAsString();
              var root = objectMapper.readTree(json);
              boolean hasErrors = root.has("errors");
              boolean hasDetails = root.has("details");
              assertThat(hasErrors || hasDetails).as("errors か details のどちらかは必須").isTrue();
              if (hasErrors) {
                assertThat(root.get("errors").isArray()).isTrue();
              }
              if (hasDetails) {
                assertThat(root.get("details").isArray()).isTrue();
              }
            })
        // 旧キーは存在しない前提を維持
        .andExpect(jsonPath("$.errorCode").doesNotExist())
        .andExpect(jsonPath("$.errorType").doesNotExist());
  }

  /**
   * 400 Bad Request レスポンスにおいて、レガシーなキー（errorCode, errorType）が 含まれていないことを確認します。
   *
   * @throws Exception MockMvc実行時例外
   */
  @Test
  void エラーレスポンスにレガシーなキーが含まれないこと() throws Exception {
    // 適当なエラーを発生させる（例：型不一致）
    mockMvc
        .perform(
            patch("/api/students/{id}", studentById)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("EMPTY_OBJECT"))
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.errorCode").doesNotExist())
        .andExpect(jsonPath("$.errorType").doesNotExist());
  }

  /**
   * バリデーション詳細（details や errors）が存在しないエラーレスポンスにおいて、 それらのキー自体がJSONに含まれないことを確認します。
   *
   * @throws Exception MockMvc実行時例外
   */
  @Test
  void 詳細情報なしのエラーでエイリアスフィールドが出力されないこと() throws Exception {
    mockMvc
        .perform(
            patch("/api/students/{id}", studentById)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("EMPTY_OBJECT"))
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.details").doesNotExist())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.errorCode").doesNotExist())
        .andExpect(jsonPath("$.errorType").doesNotExist());
  }

  /**
   * エラーレスポンスのスキーマ（契約）が、定義された最終キーのみを含み、 レガシーなエイリアスキーを含まないことを厳密に検証します。
   *
   * <p>必須キー（error, code, message）の存在と型、および レガシーキー（errorCode, errorType）や 可変キー（details,
   * errors）がこのケースでは存在しないことを確認します。
   *
   * @throws Exception MockMvc実行時例外
   */
  @Test
  void エラー契約が最終キーのみを持ちエイリアスを含まないこと() throws Exception {
    mockMvc
        .perform(
            patch("/api/students/{id}", studentById)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        // 最終キーだけ存在
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.code").exists())
        .andExpect(jsonPath("$.message").exists())
        // 最終キーの型も固定化（回 regress 防止）
        .andExpect(jsonPath("$.error").isString())
        .andExpect(jsonPath("$.code").isString())
        .andExpect(jsonPath("$.message").isString())
        // 可変（存在しないことを期待）:
        .andExpect(jsonPath("$.details").doesNotExist())
        .andExpect(jsonPath("$.errors").doesNotExist())
        // alias（レガシー）は常に無し
        .andExpect(jsonPath("$.errorCode").doesNotExist())
        .andExpect(jsonPath("$.errorType").doesNotExist());
  }

  /**
   * [400] PATCHリクエストのボディが空（null）の場合。 E003 (MISSING_PARAMETER) が返され、 そのスキーマが固定されていることを検証します。
   *
   * @throws Exception MockMvc実行時例外
   */
  @Test
  void PATCHボディ無しで400_MISSING_PARAMETER_E003が返ること() throws Exception {
    mockMvc
        .perform(patch("/api/students/{id}", studentById).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("MISSING_PARAMETER"))
        .andExpect(jsonPath("$.code").value("E003"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.details").doesNotExist())
        .andExpect(jsonPath("$.errorCode").doesNotExist())
        .andExpect(jsonPath("$.errorType").doesNotExist());
  }

  /**
   * [400] PATCHリクエストのボディが空のJSON（{}）の場合。 E006 (EMPTY_OBJECT) が返され、 そのスキーマが固定されていることを検証します。
   *
   * @throws Exception MockMvc実行時例外
   */
  @Test
  void patch_スキーマが固定の状態で_空のJSONをリクエスト_400_空のオブジェクトが返ること() throws Exception {
    mockMvc
        .perform(
            patch("/api/students/{id}", studentById)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("EMPTY_OBJECT"))
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.details").doesNotExist())
        .andExpect(jsonPath("$.errorCode").doesNotExist())
        .andExpect(jsonPath("$.errorType").doesNotExist());
  }

  /**
   * [400] リクエストボディが不正なJSON形式（例: "{"）の場合。 E002 (INVALID_JSON) が返され、 そのスキーマが固定されていることを検証します。
   *
   * @throws Exception MockMvc実行時例外
   */
  @Test
  void patch_スキーマが固定の状態で_不正なJSONをリクエスト_400_無効なJSONが返ること() throws Exception {
    mockMvc
        .perform(
            patch("/api/students/{id}", studentById)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_JSON"))
        .andExpect(jsonPath("$.code").value("E002"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.details").doesNotExist())
        .andExpect(jsonPath("$.errorCode").doesNotExist())
        .andExpect(jsonPath("$.errorType").doesNotExist());
  }

  /**
   * [404] 存在しないパス（エンドポイント）にアクセスした場合。 E404 (NOT_FOUND) が返され、 そのスキーマが固定されていることを検証します。
   *
   * @throws Exception MockMvc実行時例外
   */
  @Test
  void スキーマが見つからない場合_E404_データが存在しないエラーを返すこと() throws Exception {
    mockMvc
        .perform(get("/no/such/path"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"))
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.details").doesNotExist())
        .andExpect(jsonPath("$.errorType").doesNotExist())
        .andExpect(jsonPath("$.errorCode").doesNotExist());
  }

  // 500: 想定外例外 → INTERNAL_SERVER_ERROR / E999（makeConverterThrowEarly を利用）
  @Test
  void patch_スキーマが固定されている状態で_予期せぬ例外が発生した場合_500エラーを返すこと() throws Exception {
    Mockito.reset(service, converter);

    // ★ decode を通す（resetしたので必須）
    when(converter.decodeUuidStringOrThrow(anyString())).thenReturn(UUID.fromString(studentById));

    // ★ service入口で確実に落とす
    doThrow(new RuntimeException("boom"))
        .when(service)
        .patchStudent(any(UUID.class), any(StudentRegistrationRequest.class), anyString());

    String json =
        """
            {
              "student": { "fullName":"x" }
            }
            """;

    // Act & Assert
    mockMvc
        .perform(
            patch("/api/students/{studentId}", studentById)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.code").value("E999"));

    verify(service)
        .patchStudent(any(UUID.class), any(StudentRegistrationRequest.class), anyString());
  }
}
