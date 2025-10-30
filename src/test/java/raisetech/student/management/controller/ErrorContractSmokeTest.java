package raisetech.student.management.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import raisetech.student.management.data.Student;

class ErrorContractSmokeTest extends ControllerTestBase {

  @BeforeEach
  void setUpSmoke() {
    // ベースの @BeforeEach で reset 済みなので、共通スタブを流し込む
    stubConverterHappyPath();
    stubServiceHappyPath();

    // 念のため：NotFound させない
    when(service.findStudentById(any())).thenReturn(new Student());
  }

  @Test
  void errorSchema_isConsistent_forTypeMismatch() throws Exception {
    mockMvc.perform(get("/api/students").param("includeDeleted", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").isNumber())
        .andExpect(jsonPath("$.code").isString())
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        // ここを柔軟に：
        .andExpect(result -> {
          var json = result.getResponse().getContentAsString();
          var root = objectMapper.readTree(json);
          boolean hasErrors = root.has("errors");
          boolean hasDetails = root.has("details");
          assertThat(hasErrors || hasDetails)
              .as("errors か details のどちらかは必須")
              .isTrue();
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

  // 400のスモーク（レガシーキーが無いことの確認）
  @Test
  void errorResponse_schema_is_stable_without_legacy_keys() throws Exception {
    // 適当なエラーを発生させる（例：型不一致）
    mockMvc.perform(
            patch("/api/students/{id}", base64Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("EMPTY_OBJECT"))
        .andExpect(jsonPath("$.code").value("E003"))
        .andExpect(jsonPath("$.errorCode").doesNotExist())
        .andExpect(jsonPath("$.errorType").doesNotExist());
  }

  // 400のスモーク（details/errorsが無いときに alias が出ないこと）
  @Test
  void errorResponse_has_no_alias_fields_when_no_details() throws Exception {
    mockMvc.perform(
            patch("/api/students/{id}", base64Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("EMPTY_OBJECT"))
        .andExpect(jsonPath("$.code").value("E003"))
        .andExpect(jsonPath("$.details").doesNotExist())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.errorCode").doesNotExist())
        .andExpect(jsonPath("$.errorType").doesNotExist());
  }

  @Test
  void error_contract_has_only_final_keys_and_no_alias() throws Exception {
    mockMvc.perform(patch("/api/students/{id}", base64Id)
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
}

