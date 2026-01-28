package raisetech.student.management.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import raisetech.student.management.repository.StudentCourseRepository;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class StudentPatchRollbackIntegrationTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  StudentCourseRepository courseRepository;

  /**
   * statusがDB制約で例外が起きたとき、courseが増えていない（＝同一トランザクションでロールバック）ことを検証します。
   *
   * <p>PATCHで新規コース追加を行い、applicationStatusに21文字の値を与えてDBのVARCHAR(20)制約違反を発生させます。
   * 　例外により処理が失敗した場合でも、courseのINSERTが部分的にコミットされず、件数が増えないことを確認します。</p>
   *
   * @throws Exception MockMvc実行時例外
   */
  @Test
  void 新規コースの更新でステータスが長すぎる場合_ロールバックされ_コースが作成されないこと()
      throws Exception {
    UUID studentId = UUID.fromString("19d8186d-b15e-454e-80ef-4c8f4d550dd8");

    int before = courseRepository.countCoursesByStudentId(studentId);

    String tooLongStatus = "XXXXXXXXXXXXXXXXXXXXX"; // 21文字

    String body = """
        {
          "appendCourses": true,
          "courses": [
            { "courseName": "rollback-test", "applicationStatus": "%s" }
          ]
        }
        """.formatted(tooLongStatus);

    mockMvc.perform(patch("/api/students/{studentId}", studentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().is5xxServerError());

    int after = courseRepository.countCoursesByStudentId(studentId);
    assertThat(after).isEqualTo(before); // ロールバックで増えていないこと
  }
}
