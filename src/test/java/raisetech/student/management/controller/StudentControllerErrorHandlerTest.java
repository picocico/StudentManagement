package raisetech.student.management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
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
   * <ul>
   *   <li>{@code converter.decodeUuidStringOrThrow(studentId)} が 16バイトの受講生IDを返す
   *   <li>{@code service.findStudentById(UUID)} が {@link ResourceNotFoundException} を送出する
   * </ul>
   *
   * <p>When:
   * <ul>
   *   <li> MockMvc で {@code GET /api/students/{studentId}} を実行する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 404 が返る
   *   <li>レスポンスボディの {@code status} が 404、{@code code} が {@code "E404"} である
   *   <li>{@code error} が {@code "NOT_FOUND"} である
   *   <li>{@code message} に {@code "IDが見つかりません"} を含むことを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void getStudentDetail_存在しないIDを指定した場合_404エラーが返ること() throws Exception {

    // given
    String notFoundId = "123e4567-e89b-12d3-a456-426614174999"; // 適当なUUID文字列
    UUID notFoundUuid = UUID.fromString(notFoundId);

    // String -> UUID デコード
    when(converter.decodeUuidStringOrThrow(notFoundId)).thenReturn(notFoundUuid);
    // その UUID で検索したときに 404 ドメイン例外を投げる
    when(service.findStudentById(notFoundUuid))
        .thenThrow(new ResourceNotFoundException("student", "studentId"));

    // when & then
    mockMvc
        .perform(get("/api/students/{studentId}", notFoundId))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("E404"))        // あなたの GlobalExceptionHandler の定義に合わせる
        .andExpect(jsonPath("$.error").value("NOT_FOUND"))  // ← ここもプロジェクトの仕様に合わせて
        .andExpect(jsonPath("$.message").value(
            org.hamcrest.Matchers.containsString("student")));

    verify(converter).decodeUuidStringOrThrow(notFoundId);
    verify(service).findStudentById(notFoundUuid);
    // 404 なので searchCoursesByStudentId, toDetailDto などは呼ばれないのが理想
    verifyNoMoreInteractions(service, converter);
  }

  /**
   * studentId を指定せずに受講生詳細のルートを叩いた場合、404（NOT_FOUND）が返ることを検証します。
   *
   * <p>Endpoint: {@code GET /api/students/}（パス変数なし） <br>
   * Status: {@code 404 NOT_FOUND}
   *
   * <p>When:
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/} を実行する（{@code {studentId}} を指定しない）
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 404 が返る
   *   <li>レスポンスボディの {@code status} が 404、{@code code} が {@code "E404"} である
   *   <li>{@code error} が {@code "NOT_FOUND"} である
   *   <li>{@code message} に {@code "URLは存在しません"} を含むことを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void getStudentDetail_studentIdを指定しない場合_404エラーが返ること() throws Exception {

    mockMvc
        .perform(get("/api/students/"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value(containsString("URLは存在しません")));
  }

  /**
   * 存在しない受講生IDで更新した場合に 404（E404/NOT_FOUND）が返ることを検証します。
   *
   * <p>Endpoint: {@code PUT /api/students/{studentId}}<br>
   * Status: {@code 404 NOT_FOUND}
   *
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeUuidStringOrThrow(studentId)} が UUIDの受講生IDを返す</li>
   *   <li>受講生・コース変換 {@code converter.toEntity(...)} および
   *       {@code converter.toEntityList(...)} は正常に完了する</li>
   *   <li>{@code service.updateStudentWithCourses(...)} が
   *       {@link ResourceNotFoundException} を送出する</li>
   * </ul>
   *
   * <p>When:
   * <ul>
   *   <li>MockMvc で {@code PUT /api/students/{studentId}} を実行する</li>
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 404 が返る</li>
   *   <li>レスポンスボディの {@code status} が 404、{@code code} が {@code "E404"} である</li>
   *   <li>{@code error} が {@code "NOT_FOUND"} である</li>
   *   <li>{@code message} に {@code "IDが見つかりません"} を含むことを検証する</li>
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
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

    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);
    when(converter.toEntity(studentDto)).thenReturn(student);
    when(converter.toEntityList(eq(List.of()), argThat(id -> Objects.equals(id, studentId))))
        .thenReturn(List.of());

    // 更新後の再取得で見つからないケースを想定
    doNothing().when(service).updateStudent(any(Student.class), anyList());
    doThrow(new ResourceNotFoundException("IDが見つかりません"))
        .when(service)
        .updateStudentWithCourses(any(Student.class), anyList());

    mockMvc
        .perform(
            put("/api/students/{studentId}", studentById)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valid))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value(containsString("IDが見つかりません")));

    verify(converter).decodeUuidStringOrThrow(studentById);
  }

  /**
   * 更新処理中の想定外例外を 500（E999/INTERNAL_SERVER_ERROR）としてハンドリングすることを検証します。
   *
   * <p>Endpoint: {@code PUT /api/students/{studentId}} <br>
   * Status: {@code 500 INTERNAL_SERVER_ERROR}
   *
   * <p>Given:
   * <ul>
   *   <li>受講生IDのデコード（{@code converter.decodeUuidStringOrThrow(studentId)}）および DTO → エンティティ変換は成功する
   *   <li>{@code service.updateStudentWithCourses(...)} が実行時例外を送出する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 500 が返る
   *   <li>レスポンスボディの {@code status} が 500、{@code code} が {@code "E999"} である
   *   <li>{@code error} が {@code "INTERNAL_SERVER_ERROR"} である
   *   <li>想定外例外発生のため、{@code StudentConverter} を用いたレスポンス組み立て
   *   IDエンコード／3引数版 {@code toDetailDto(...)} 等が呼び出されないことを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  void updateStudent_想定外の例外が発生した場合_500を返すこと() throws Exception {
    // Arrange: 正常リクエストを用意（バリデーションは通す）
    var req = new StudentRegistrationRequest();
    req.setStudent(studentDto); // @BeforeEach のものを利用
    req.setCourses(List.of(courseDto)); // null ではなく、1件以上を入れて通過

    // 変換系のモック（ここまでは正常に進む）
    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);
    when(converter.toEntity(studentDto)).thenReturn(student);
    when(converter.toEntityList(
        eq(List.of(courseDto)), argThat(id -> Objects.equals(id, studentId))))
        .thenReturn(courses);

    // Service の更新時に想定外例外を発生させる
    doThrow(new RuntimeException("Unexpected failure"))
        .when(service)
        .updateStudentWithCourses(any(Student.class), anyList());

    // Act & Assert
    mockMvc
        .perform(
            put("/api/students/{studentId}", studentById)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.code").value("E999")) // ← 文字列
        .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.message").value(containsString("エラー")));

    // 呼び出し検証（更新で落ちたので DTO 化までは行かない）
    verify(converter).decodeUuidStringOrThrow(studentById);
    verify(converter).toEntity(studentDto);
    verify(converter).toEntityList(anyList(), argThat(id -> id.equals(studentId)));

    verify(service).updateStudentWithCourses(any(Student.class), anyList());

    // ★到達しないことを明示
    verify(converter, never()).encodeUuidString(any(UUID.class));
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
   * <ul>
   *   <li>{@code converter.decodeUuidStringOrThrow(studentId)} により受講生IDが正しくデコードされる
   *   <li>{@code service.findStudentById(studentId)} が {@link ResourceNotFoundException} を送出する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 404 が返る
   *   <li>{@code status}=404, {@code code}={@code "E404"}, {@code error}={@code "NOT_FOUND"} がレスポンスに含まれる
   *   <li>{@code message} に {@code "見つかりません"} を含むことを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void partialUpdateStudent_該当する受講生情報が存在しない場合_404を返すこと()
      throws Exception {

    StudentRegistrationRequest request = new StudentRegistrationRequest();
    request.setStudent(studentDto);
    request.setCourses(List.of(courseDto));
    request.setAppendCourses(false);

    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);
    when(service.findStudentById(studentId))
        .thenThrow(new ResourceNotFoundException("該当する受講生が見つかりません"));

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/api/students/{studentId}", studentById)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value(containsString("見つかりません")));
  }

  /**
   * （置換モード）部分更新の途中で想定外の例外が発生した場合に 500 を返すことを検証します。
   *
   * <p>Endpoint: {@code PATCH /api/students/{studentId}} <br>
   * Status: {@code 500 INTERNAL_SERVER_ERROR}
   *
   * <p>Given:
   * <ul>
   *   <li>受講生IDのデコードおよび既存受講生の取得が成功する
   *   <li>DTO → エンティティ変換とマージ処理は成功する
   *   <li>{@code appendCourses} が未指定でデフォルト {@code false}（置換モード）となる
   *   <li>{@code service.replaceCourses(studentId, ...)} が実行時例外を送出する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 500 が返る
   *   <li>{@code service.replaceCourses(...)} が期待通りの引数で呼び出されることを検証する
   *   <li>想定外例外発生のため {@code updateStudentInfoOnly(...)} や
   *   {@code searchCoursesByStudentId(...)} が呼び出されないことを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void partialUpdateStudent_想定外の例外が発生した場合_500を返すこと() throws Exception {

    String body = json(Map.of("student", studentDto, "courses", List.of(courseDto)));

    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);
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
            MockMvcRequestBuilders.patch("/api/students/{studentId}", studentById)
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
   * <ul>
   *   <li>{@code converter.decodeUuidStringOrThrow(studentId)} により受講生IDが正しくデコードされる
   *   <li>{@code service.softDeleteStudent(studentId)} が {@link ResourceNotFoundException} を送出する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 404 が返る
   *   <li>レスポンスボディの {@code code} が {@code "E404"}、{@code error} が {@code "NOT_FOUND"} である
   *   <li>{@code softDeleteStudent} 以外に余計なサービス呼び出しが行われないことを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void deleteStudent_対象受講生が存在しない場合_404を返すこと() throws Exception {

    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);
    doThrow(new ResourceNotFoundException("IDが見つかりません"))
        .when(service)
        .softDeleteStudent(eq(studentId));

    mockMvc
        .perform(delete("/api/students/{studentId}", studentById))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));

    verify(converter).decodeUuidStringOrThrow(studentById);
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
   * <ul>
   *   <li>{@code converter.decodeUuidStringOrThrow(studentId)} により受講生IDが正しくデコードされる
   *   <li>{@code service.softDeleteStudent(studentId)} が実行時例外を送出する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 500 が返る
   *   <li>レスポンスボディの {@code code} が {@code "E999"}、{@code error} が
   *   {@code "INTERNAL_SERVER_ERROR"} である
   *   <li>{@code softDeleteStudent} 以外に余計なサービス呼び出しが行われないことを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void deleteStudent_想定外の例外が発生した場合_500を返すこと() throws Exception {

    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);
    doThrow(new RuntimeException("Unexpected failure"))
        .when(service)
        .softDeleteStudent(eq(studentId));

    mockMvc
        .perform(delete("/api/students/{studentId}", studentById))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.code").value("E999"));

    verify(converter).decodeUuidStringOrThrow(studentById);
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
   * <ul>
   *   <li>{@code converter.decodeUuidStringOrThrow(studentId)} により受講生IDが正しくデコードされる
   *   <li>{@code service.restoreStudent(studentId)} が {@link ResourceNotFoundException} を送出する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 404 が返る
   *   <li>レスポンスボディの {@code code} が {@code "E404"}、{@code error} が {@code "NOT_FOUND"} である
   *   <li>{@code restoreStudent} 以外に余計なサービス呼び出しが行われないことを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void restoreStudent_対象受講生が存在しないまたは未削除の場合_404を返すこと()
      throws Exception {

    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);
    doThrow(new ResourceNotFoundException("IDが見つかりません"))
        .when(service)
        .restoreStudent(eq(studentId));

    mockMvc
        .perform(MockMvcRequestBuilders.patch("/api/students/{studentId}/restore", studentById))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));

    verify(converter).decodeUuidStringOrThrow(studentById);
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
   * <ul>
   *   <li>{@code converter.decodeUuidStringOrThrow(studentId)} により受講生IDが正しくデコードされる
   *   <li>{@code service.restoreStudent(studentId)} が実行時例外を送出する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 500 が返る
   *   <li>レスポンスボディの {@code code} が {@code "E999"}、{@code error} が
   *   {@code "INTERNAL_SERVER_ERROR"} である
   *   <li>{@code restoreStudent} 以外に余計なサービス呼び出しが行われないことを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void restoreStudent_想定外の例外が発生した場合_500を返すこと() throws Exception {

    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);
    doThrow(new RuntimeException("Unexpected failure")).when(service)
        .restoreStudent(eq(studentId));

    mockMvc
        .perform(patch("/api/students/{studentId}/restore", studentById))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.code").value("E999"));

    verify(converter).decodeUuidStringOrThrow(studentById);
    verify(service).restoreStudent(eq(studentId));
    verifyNoMoreInteractions(service);
  }

  // ------------------------
  // ▼ 以下：共通例外ハンドラのWebテスト
  // ------------------------

  /**
   * 必須リクエストパラメータ欠如時（MissingServletRequestParameterException）が 400/E003 としてハンドリングされることを検証します。
   *
   * <p>Endpoint: {@code GET /api/students/test-missing-param} <br>
   * Status: {@code 400 BAD_REQUEST}
   *
   * <p>When:
   * <ul>
   *   <li>必須のクエリパラメータ {@code keyword} を付与せずにエンドポイントを呼び出す
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 400 が返る
   *   <li>レスポンスボディの {@code code} が {@code "E003"} であることを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void testMissingServletRequestParameterException() throws Exception {
    mockMvc
        .perform(get("/api/students/test-missing-param")) // keyword を指定しない
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E003")); // GlobalExceptionHandler 側のエラーコードに応じて変更
  }

  /**
   * 型不一致時（MethodArgumentTypeMismatchException）が 400/E004 としてハンドリングされることを検証します。
   *
   * <p>Endpoint: {@code GET /api/students/test-type} <br>
   * Status: {@code 400 BAD_REQUEST}
   *
   * <p>When:
   * <ul>
   *   <li>{@code int} 変換が必要な {@code id} パラメータに数値以外（例: {@code "abc"}}）を指定する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 400 が返る
   *   <li>レスポンスボディの {@code code} が {@code "E004"} であることを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void testMethodArgumentTypeMismatchException() throws Exception {
    mockMvc
        .perform(get("/api/students/test-type").param("id", "abc")) // int 型に変換できない文字列
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E004"));
  }
}
