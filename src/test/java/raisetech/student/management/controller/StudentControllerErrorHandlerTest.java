package raisetech.student.management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.exception.ResourceNotFoundException;

class StudentControllerErrorHandlerTest extends ControllerTestBase {

  /**
   * 存在しない受講生IDを指定した場合に 404（E404/NOT_FOUND）が返却されることを検証します。
   *
   * <p>Endpoint: {@code GET /api/students/{studentId}} <br>
   * Status: {@code 404 NOT_FOUND}
   *
   * <p>Given:
   *
   * <ul>
   *   <li>{@code converter.decodeBase64(base64Id)} が 16バイトID を返す
   *   <li>{@code service.findStudentById(studentId)} が {@code ResourceNotFoundException} を送出
   * </ul>
   * <p>
   * When:
   *
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/{studentId}} を実行
   * </ul>
   * <p>
   * Then:
   *
   * <ul>
   *   <li>status=404, code=E404, error=NOT_FOUND を検証する
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentDetail_存在しないIDを指定した場合_404エラーが返ること() throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    when(service.findStudentById(studentId)).thenThrow(
        new ResourceNotFoundException("IDが見つかりません"));

    mockMvc
        .perform(get("/api/students/{studentId}", base64Id))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND")) // ← NOT_FOUND → NOT_FOUND
        .andExpect(jsonPath("$.message").value(containsString("IDが見つかりません")));
  }

  /**
   * studentId を指定せずにコレクションのルートを叩いた場合、404（NOT_FOUND）が返ることを検証します。
   *
   * <p>Endpoint: {@code GET /api/students/}（パス変数未指定） <br>
   * Status: {@code 404 NOT_FOUND}
   *
   * <p>When:
   *
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/} を実行
   * </ul>
   * <p>
   * Then:
   *
   * <ul>
   *   <li>status=404, code=E404, error=NOT_FOUND を検証する
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentDetail_studentIdが空文字の場合_404エラーが返ること() throws Exception {

    mockMvc
        .perform(get("/api/students/"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND")) // ← NOT_FOUND ではない
        .andExpect(jsonPath("$.message").value(containsString("URLは存在しません")));
  }

  /**
   * 存在しない受講生IDで更新した場合に 404（E404/NOT_FOUND）が返ることを検証します。
   *
   * <p>Endpoint: {@code PUT /api/students/{studentId}} <br>
   * Status: {@code 404 NOT_FOUND}
   *
   * <p>Given:
   *
   * <ul>
   *   <li>{@code converter.decodeUuidOrThrow(base64Id)} は有効IDを返す
   *   <li>{@code service.updateStudentWithCourses(...)} が {@code ResourceNotFoundException} を送出
   * </ul>
   * <p>
   * When:
   *
   * <ul>
   *   <li>MockMvcでPUTを実行
   * </ul>
   * <p>
   * Then:
   *
   * <ul>
   *   <li>status=404, code=E404, error=NOT_FOUND を検証
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_存在しない受講生IDを指定した場合_404を返すこと() throws Exception {
    var valid =
        json(
            new StudentRegistrationRequest() {
              {
                setStudent(studentDto);
                setCourses(List.of());
              }
            });

    when(idCodec.decodeUuidOrThrow(base64Id)).thenReturn(UUID.fromString(VALID_UUID));
    when(converter.toEntity(studentDto)).thenReturn(student);
    when(converter.toEntityList(eq(List.of()), argThat(arr -> Arrays.equals(arr, studentId))))
        .thenReturn(List.of());

    // 更新後の再取得で見つからないケースを想定
    doNothing().when(service).updateStudent(any(Student.class), anyList());
    doThrow(new ResourceNotFoundException("IDが見つかりません"))
        .when(service)
        .updateStudentWithCourses(any(Student.class), anyList());

    mockMvc
        .perform(
            put("/api/students/{studentId}", base64Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valid))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND")) // ← NOT_FOUND ではない
        .andExpect(jsonPath("$.message").value(containsString("IDが見つかりません")));
  }

  /**
   * 更新処理中の想定外例外を 500（E999/INTERNAL_SERVER_ERROR）としてハンドリングすることを検証します。
   *
   * <p>Endpoint: {@code PUT /api/students/{studentId}} <br>
   * Status: {@code 500 INTERNAL_SERVER_ERROR}
   *
   * <p>Given:
   *
   * <ul>
   *   <li>前段の変換は成功する
   *   <li>{@code service.updateStudentWithCourses(...)} が実行時例外を送出
   * </ul>
   * <p>
   * Then:
   *
   * <ul>
   *   <li>status=500, code=E999, error=INTERNAL_SERVER_ERROR を検証
   *   <li>{@code encodeBase64} / 3引数版 {@code toDetailDto} は呼ばれない
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_想定外の例外が発生した場合_500を返すこと() throws Exception {
    // Arrange: 正常リクエストを用意（バリデーションは通す）
    var req = new StudentRegistrationRequest();
    req.setStudent(studentDto); // @BeforeEach のものを利用
    req.setCourses(List.of(courseDto)); // null ではなく、1件以上を入れて通過

    // 変換系のモック（ここまでは正常に進む）
    when(idCodec.decodeUuidOrThrow(base64Id)).thenReturn(UUID.fromString(VALID_UUID));
    when(converter.toEntity(studentDto)).thenReturn(student);
    when(converter.toEntityList(
        eq(List.of(courseDto)), argThat(arr -> Arrays.equals(arr, studentId))))
        .thenReturn(courses);

    // ★ ここで expected を用意（JSONの期待内容に合わせる）
    StudentDetailDto expected = new StudentDetailDto(studentDto, List.of(courseDto));

    // ★ 3引数版 toDetailDto の stub
    when(converter.toDetailDto(
        any(Student.class), // service が別インスタンスを返す可能性に備えて any()
        same(courses), // toEntityList の戻りをそのまま使うなら same() が安全
        eq(base64Id) // encodeBase64 の戻り
    ))
        .thenReturn(expected);
    // Service の更新時に想定外例外を発生させる
    doThrow(new RuntimeException("Unexpected failure"))
        .when(service)
        .updateStudentWithCourses(any(Student.class), anyList());

    // Act & Assert
    mockMvc
        .perform(
            put("/api/students/{studentId}", base64Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.code").value("E999")) // ← 文字列
        .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.message").value(containsString("エラー")));

    // 呼び出し検証（更新で落ちたので DTO 化までは行かない）
    verify(idCodec).decodeUuidBytesOrThrow(base64Id);
    verify(converter).toEntity(studentDto);
    verify(converter).toEntityList(anyList(), argThat(arr -> Arrays.equals(arr, studentId)));

    verify(service).updateStudentWithCourses(any(Student.class), anyList());

    // ★到達しないことを明示
    verify(converter, never()).encodeBase64(any(byte[].class));
    verify(converter, never()).toDetailDto(any(), anyList(), anyString());

    // 余計な呼び出しが無いこと
    verifyNoMoreInteractions(converter, service);
  }

  /**
   * 部分更新で該当受講生が存在しない場合に 404（E404/NOT_FOUND）が返ることを検証します。
   *
   * <p>Endpoint: {@code PATCH /api/students/{studentId}} <br>
   * Status: {@code 404 NOT_FOUND}
   *
   * <p>Given:
   *
   * <ul>
   *   <li>{@code service.findStudentById(studentId)} が {@code ResourceNotFoundException} を送出
   * </ul>
   * <p>
   * Then:
   *
   * <ul>
   *   <li>status=404, code=E404, error=NOT_FOUND, メッセージ本文を検証
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void partialUpdateStudent_該当する受講生情報が存在しない場合_404を返すこと()
      throws Exception {

    StudentRegistrationRequest request = new StudentRegistrationRequest();
    request.setStudent(studentDto);
    request.setCourses(List.of(courseDto));
    request.setAppendCourses(false);

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    when(service.findStudentById(studentId))
        .thenThrow(new ResourceNotFoundException("該当する受講生が見つかりません"));

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/api/students/{studentId}", base64Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND")) // ← NOT_FOUND ではなくこれ
        .andExpect(jsonPath("$.message").value(containsString("見つかりません")));
  }

  /**
   * （置換モード）部分更新の途中で想定外の例外が発生した場合に 500 を返すことを検証します。
   *
   * <p>Endpoint: {@code PATCH /api/students/{studentId}} Status: {@code 500 Internal Server Error}
   *
   * <p>Given: - student と courses を含む有効な JSON - 変換・マージは成功するが、置換処理中に例外が発生
   *
   * <p>Then: - {@code service.replaceCourses(...)} が呼ばれる - 想定外例外により 500 が返る - 以降の処理（{@code
   * updateStudentInfoOnly} 等）は呼ばれない
   */
  @Test
  public void partialUpdateStudent_想定外の例外が発生した場合_500を返すこと() throws Exception {

    String body = json(Map.of("student", studentDto, "courses", List.of(courseDto)));

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    Student existing = new Student();
    when(service.findStudentById(studentId)).thenReturn(existing);

    Student merged = new Student();
    when(converter.toEntity(studentDto)).thenReturn(merged);
    doNothing().when(converter).mergeStudent(existing, merged);

    List<StudentCourse> newCourses = List.of(new StudentCourse());
    when(converter.toEntityList(List.of(courseDto), studentId)).thenReturn(newCourses);

    // append 未指定 → デフォルト false（置換ルート）で「想定外例外」を発生させる
    doThrow(new RuntimeException("Unexpected failure"))
        .when(service)
        .replaceCourses(eq(studentId), ArgumentMatchers.anyList());

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/api/students/{studentId}", base64Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isInternalServerError());

    // appendCourses 未指定 ⇒ デフォルト(false) ⇒ 置換モード
    verify(service)
        .replaceCourses(
            eq(studentId),
            argThat(
                list ->
                    list != null
                        && list.size() == newCourses.size()
                        && list.containsAll(newCourses)
                        && newCourses.containsAll(list)));

    // 例外で以降は実行されない想定
    verify(service, never()).updateStudentInfoOnly(any());
    verify(service, never()).searchCoursesByStudentId(any());
  }

  /**
   * 論理削除対象が存在しない場合に 404（E404/NOT_FOUND）が返ることを検証します。
   *
   * <p>Endpoint: {@code DELETE /api/students/{studentId}} <br>
   * Status: {@code 404 NOT_FOUND}
   *
   * <p>Given:
   *
   * <ul>
   *   <li>{@code service.softDeleteStudent(studentId)} が {@code ResourceNotFoundException} を送出
   * </ul>
   * <p>
   * Then:
   *
   * <ul>
   *   <li>status=404, code=E404, error=NOT_FOUND を検証
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void deleteStudent_対象受講生が存在しない場合_404を返すこと() throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    doThrow(new ResourceNotFoundException("IDが見つかりません"))
        .when(service)
        .softDeleteStudent(eq(studentId));

    mockMvc
        .perform(delete("/api/students/{studentId}", base64Id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));

    verify(converter).decodeBase64(base64Id);
    verify(service).softDeleteStudent(eq(studentId));
    verifyNoMoreInteractions(service);
  }

  /**
   * 論理削除処理中の想定外例外を 500（E999/INTERNAL_SERVER_ERROR）としてハンドリングすることを検証します。
   *
   * <p>Endpoint: {@code DELETE /api/students/{studentId}} <br>
   * Status: {@code 500 INTERNAL_SERVER_ERROR}
   *
   * <p>Given:
   *
   * <ul>
   *   <li>{@code service.softDeleteStudent(studentId)} が実行時例外を送出
   * </ul>
   * <p>
   * Then:
   *
   * <ul>
   *   <li>status=500, code=E999, error=INTERNAL_SERVER_ERROR を検証
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void deleteStudent_想定外の例外が発生した場合_500を返すこと() throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    doThrow(new RuntimeException("Unexpected failure"))
        .when(service)
        .softDeleteStudent(eq(studentId));

    mockMvc
        .perform(delete("/api/students/{studentId}", base64Id))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.code").value("E999"));

    verify(converter).decodeBase64(base64Id);
    verify(service).softDeleteStudent(eq(studentId));
    verifyNoMoreInteractions(service);
  }

  /**
   * 復元対象が存在しない／未削除の場合に 404（E404/NOT_FOUND）が返ることを検証します。
   *
   * <p>Endpoint: {@code PATCH /api/students/{studentId}/restore} <br>
   * Status: {@code 404 NOT_FOUND}
   *
   * <p>Given:
   *
   * <ul>
   *   <li>{@code service.restoreStudent(studentId)} が {@code ResourceNotFoundException} を送出
   * </ul>
   * <p>
   * Then:
   *
   * <ul>
   *   <li>status=404, code=E404, error=NOT_FOUND を検証
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void restoreStudent_対象受講生が存在しないまたは未削除の場合_404を返すこと()
      throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    doThrow(new ResourceNotFoundException("IDが見つかりません"))
        .when(service)
        .restoreStudent(eq(studentId));

    mockMvc
        .perform(MockMvcRequestBuilders.patch("/api/students/{studentId}/restore", base64Id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));

    verify(converter).decodeBase64(base64Id);
    verify(service).restoreStudent(eq(studentId));
    verifyNoMoreInteractions(service);
  }

  /**
   * 復元処理中の想定外例外を 500（E999/INTERNAL_SERVER_ERROR）としてハンドリングすることを検証します。
   *
   * <p>Endpoint: {@code PATCH /api/students/{studentId}/restore} <br>
   * Status: {@code 500 INTERNAL_SERVER_ERROR}
   *
   * <p>Given:
   *
   * <ul>
   *   <li>{@code service.restoreStudent(studentId)} が実行時例外を送出
   * </ul>
   * <p>
   * Then:
   *
   * <ul>
   *   <li>status=500, code=E999, error=INTERNAL_SERVER_ERROR を検証
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void restoreStudent_想定外の例外が発生した場合_500を返すこと() throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    doThrow(new RuntimeException("Unexpected failure")).when(service).restoreStudent(eq(studentId));

    mockMvc
        .perform(patch("/api/students/{studentId}/restore", base64Id))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.code").value("E999"));

    verify(converter).decodeBase64(base64Id);
    verify(service).restoreStudent(eq(studentId));
    verifyNoMoreInteractions(service);
  }

  // ------------------------
  // ▼ 以下：共通例外ハンドラのWebテスト
  // ------------------------

  /**
   * MissingServletRequestParameterException のテスト。 'keyword' パラメータなしで呼び出して 400 を期待。
   * 必須パラメータ欠如時（MissingServletRequestParameterException）に 400/E003 が返ることを検証します。
   *
   * <p>Endpoint: {@code GET /api/students/test-missing-param} <br>
   * Status: {@code 400 BAD_REQUEST} When:
   *
   * <ul>
   *   <li>必須の {@code keyword} を付けずに呼び出す
   * </ul>
   * <p>
   * Then:
   *
   * <ul>
   *   <li>code=E003 を検証
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void testMissingServletRequestParameterException() throws Exception {
    mockMvc
        .perform(get("/api/students/test-missing-param")) // keyword を指定しない
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E003")); // GlobalExceptionHandler 側のエラーコードに応じて変更
  }

  /**
   * MethodArgumentTypeMismatchException のテスト。 'id' に不正な型（例: abc）を指定して 400 を期待。
   * 型不一致時（MethodArgumentTypeMismatchException）に 400/E004 が返ることを検証します。
   *
   * <p>Endpoint: {@code GET /api/students/test-type} <br>
   * Status: {@code 400 BAD_REQUEST} When:
   *
   * <ul>
   *   <li>{@code id} に数値以外（例: {@code "abc"}}）を指定
   * </ul>
   * <p>
   * Then:
   *
   * <ul>
   *   <li>code=E004 を検証
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void testMethodArgumentTypeMismatchException() throws Exception {
    mockMvc
        .perform(get("/api/students/test-type").param("id", "abc")) // int 型に変換できない文字列
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E004"));
  }
}
