package raisetech.student.management.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import raisetech.student.management.exception.ResourceNotFoundException;

/**
 * {@link raisetech.student.management.controller.admin.AdminStudentController} の振る舞いを検証するテスト。
 *
 * <p>{@link ControllerTestBase} を継承し、MockMvc と @MockBean の
 * StudentService / StudentConverter を共有して利用します。
 */
class AdminStudentControllerTest extends ControllerTestBase {

  /**
   * 正常系: 正しい UUID を指定し、サービス層が例外を投げなかった場合、 204 No Content が返ることを検証します。
   */
  @Test
  void forceDeleteStudent_正常系_204が返ること() throws Exception {
    // given
    String idStr = studentById; // ControllerTestBase で用意している UUID 文字列
    UUID idUuid = studentId;    // 同じく Base で用意している UUID

    // UUID文字列 → UUID へのデコード
    when(converter.decodeUuidStringOrThrow(idStr)).thenReturn(idUuid);
    // サービスは例外を投げずに正常終了（void メソッドなので doNothing）
    doNothing().when(service).forceDeleteStudent(idUuid);

    // when & then
    mockMvc
        .perform(delete("/admin/students/{studentId}", idStr))
        .andExpect(status().isNoContent());

    // 呼び出し検証
    verify(converter).decodeUuidStringOrThrow(idStr);
    verify(service).forceDeleteStudent(idUuid);
  }

  /**
   * 異常系: UUID 文字列として不正な ID を指定した場合、 400 Bad Request / E006 が返ることを検証します。
   *
   * <p>GlobalExceptionHandler のマッピングに合わせて assertion を調整してください。
   */
  @Test
  void forceDeleteStudent_UUID形式として不正なIDを指定した場合_400が返ること() throws Exception {
    String invalid = "@@invalid@@";

    // デコード時点で形式不正として例外を投げる想定
    when(converter.decodeUuidStringOrThrow(invalid))
        .thenThrow(new IllegalArgumentException("IDの形式が不正です（UUID）"));

    mockMvc
        .perform(delete("/admin/students/{studentId}", invalid))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E006"))              // プロジェクト仕様に合わせる
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))  // ここも仕様にあわせて
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString("IDの形式が不正")));

    verify(converter).decodeUuidStringOrThrow(invalid);
    // デコードで落ちるので service は呼ばれない想定
    verifyNoInteractions(service);
  }

  /**
   * 異常系: 存在しない ID を指定した場合、 サービス層が ResourceNotFoundException をスローし、 404 NOT_FOUND / E404
   * が返ることを検証します。
   */
  @Test
  void forceDeleteStudent_存在しないIDを指定した場合_404が返ること() throws Exception {
    String idStr = studentById;
    UUID idUuid = studentId;

    when(converter.decodeUuidStringOrThrow(idStr)).thenReturn(idUuid);
    // サービス側で 404 相当のドメイン例外をスロー
    doThrow(new ResourceNotFoundException("student", "studentId"))
        .when(service).forceDeleteStudent(idUuid);

    mockMvc
        .perform(delete("/admin/students/{studentId}", idStr))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("E404"))        // GlobalExceptionHandler に合わせる
        .andExpect(jsonPath("$.error").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("student")));

    verify(converter).decodeUuidStringOrThrow(idStr);
    verify(service).forceDeleteStudent(idUuid);
  }
}



