package raisetech.student.management.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import raisetech.student.management.config.SecurityConfig;
import raisetech.student.management.config.TestMockConfig;
import raisetech.student.management.config.security.CustomAccessDeniedHandler;
import raisetech.student.management.config.security.CustomAuthenticationEntryPoint;
import raisetech.student.management.controller.admin.AdminStudentController;
import raisetech.student.management.exception.GlobalExceptionHandler;

/**
 * Spring Securityのフィルタを有効にした状態でのエラーレスポンス契約（スキーマ）をテストするクラス。
 *
 * <p>{@link CustomAuthenticationEntryPoint} (401 Unauthorized) や {@link CustomAccessDeniedHandler}
 * (403 Forbidden) が、 {@link GlobalExceptionHandler} と同様に一貫したJSONスキーマのエラーレスポンスを 返却することを検証します。
 */
@AutoConfigureMockMvc(addFilters = true) // ← セキュリティフィルタを有効のまま
@WebMvcTest(controllers = {AdminStudentController.class})
@Import({
  // セキュリティ構成とハンドラ、例外ハンドラ、本テスト用モック
  SecurityConfig.class,
  CustomAuthenticationEntryPoint.class,
  CustomAccessDeniedHandler.class,
  GlobalExceptionHandler.class,
  TestMockConfig.class
})
@ImportAutoConfiguration(exclude = {GsonAutoConfiguration.class})
@ActiveProfiles("test")
class SecurityErrorContractTest {

  @Autowired MockMvc mockMvc;

  /**
   * [401] 未認証の状態で保護されたエンドポイント（/api/admin/**）にアクセスした際、 E401 (UNAUTHORIZED)
   * のスキーマに基づいたエラーレスポンスが返されることを検証します。
   *
   * @throws Exception MockMvc実行時例外
   * @see CustomAuthenticationEntryPoint
   */
  @Test
  void unauthorized_returns_E401_schema() throws Exception {
    // 認証なしで /api/admin/** にアクセス → 401 (UNAUTHORIZED)
    mockMvc
        .perform(
            delete("/api/admin/students/{id}", "AAAAAAAAAAAAAAAAAAAAAA")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.code").value("E401"))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.details").doesNotExist())
        .andExpect(jsonPath("$.errors").doesNotExist());
  }

  /**
   * [403] 認証済みだが権限が不足している状態（ROLE_USER）で管理エンドポイントにアクセスした際、 E403 (FORBIDDEN)
   * のスキーマに基づいたエラーレスポンスが返されることを検証します。
   *
   * @throws Exception MockMvc実行時例外
   * @see CustomAccessDeniedHandler
   */
  @Test
  void forbidden_returns_E403_schema() throws Exception {
    // 「認証済み だが 権限不足（ROLE_USER）」→ 403 (FORBIDDEN)
    mockMvc
        .perform(
            delete("/api/admin/students/{id}", "AAAAAAAAAAAAAAAAAAAAAA")
                .with(user("user").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.code").value("E403"))
        .andExpect(jsonPath("$.message").exists());
  }
}
