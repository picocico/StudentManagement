package raisetech.student.management.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentCourseDto;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentDto;
import raisetech.student.management.dto.StudentRegistrationRequest;

class StudentControllerSuccessTest extends ControllerTestBase {

  /**
   * 受講生IDで詳細を取得した際に、受講生情報とコース一覧が期待通りに返却されることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students/{studentId}}
   * <br>Status: {@code 200 OK}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(base64Id)} が 16バイトID を返す</li>
   *   <li>{@code service.findStudentById(studentId)} が既存の受講生を返す</li>
   *   <li>{@code service.searchCoursesByStudentId(studentId)} が2件のコースを返す</li>
   *   <li>{@code converter.toDetailDto(student, courses)} が期待DTOを返す</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/{studentId}} を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>HTTP 200 が返る</li>
   *   <li>student配下の各フィールドと courses の中身（件数／日付／null終端）が一致する</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentDetail_受講生ID検索した場合_一致する受講生詳細が返ること()
      throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    when(service.findStudentById(studentId)).thenReturn(student);
    when(service.searchCoursesByStudentId(studentId)).thenReturn(courses);
    when(converter.toDetailDto(student, courses)).thenReturn(
        new StudentDetailDto(studentDto, List.of(courseDto1, courseDto2)));

    mockMvc.perform(get("/api/students/{studentId}", base64Id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.student.studentId").value(base64Id))
        .andExpect(jsonPath("$.student.fullName").value(fullName))
        .andExpect(jsonPath("$.student.furigana").value(furigana))
        .andExpect(jsonPath("$.student.nickname").value(nickname))
        .andExpect(jsonPath("$.student.email").value(email))
        .andExpect(jsonPath("$.student.location").value(location))
        .andExpect(jsonPath("$.student.age").value(age))
        .andExpect(jsonPath("$.student.gender").value(gender))
        .andExpect(jsonPath("$.student.remarks").value(remarks))
        .andExpect(jsonPath("$.student.deleted").value(false))
        .andExpect(jsonPath("$.courses.length()").value(2))
        .andExpect(jsonPath("$.courses[0].courseName").value(courseName))
        .andExpect(jsonPath("$.courses[0].startDate").value("2024-03-01"))
        .andExpect(jsonPath("$.courses[1].courseName").value(secondCourseName))
        .andExpect(jsonPath("$.courses[1].startDate").value("2024-07-01"))
        .andExpect(jsonPath("$.courses[1].endDate", nullValue()));
  }

  /**
   * 受講コースが存在しない受講生IDで詳細を取得した場合、コース配列が空で返却されることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students/{studentId}}
   * <br>Status: {@code 200 OK}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(base64Id)} が 16バイトID を返す</li>
   *   <li>{@code service.findStudentById(studentId)} が既存の受講生を返す</li>
   *   <li>{@code service.searchCoursesByStudentId(studentId)} が空リストを返す</li>
   *   <li>{@code converter.toDetailDto(student, emptyList)} が期待DTOを返す</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/{studentId}} を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>HTTP 200 が返る</li>
   *   <li>{@code $.courses.length()==0} を検証する</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentDetail_受講コースが存在しない場合_空のコースリストが返ること()
      throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    when(service.findStudentById(studentId)).thenReturn(student);
    when(service.searchCoursesByStudentId(studentId)).thenReturn(Collections.emptyList());
    when(converter.toDetailDto(student, Collections.emptyList()))
        .thenReturn(new StudentDetailDto(studentDto, List.of()));

    mockMvc.perform(get("/api/students/{studentId}", base64Id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.student.fullName").value(fullName))
        .andExpect(jsonPath("$.courses.length()").value(0));
  }

  /**
   * ふりがなで検索した場合に一致する受講生リストが返却されることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students}
   * <br>Params: {@code furigana={指定値}}, {@code includeDeleted=false}, {@code deletedOnly=false}
   * <br>Status: {@code 200 OK}
   * <p>Given:
   * <ul>
   *   <li>service.getStudentList(furigana, false, false) が1件の結果を返す</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvcでGET（クエリ: furigana 等）を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>配列長が1である</li>
   *   <li>先頭要素の氏名およびコース名が期待通りである</li>
   *   <li>service.getStudentList が期待する引数で1回呼ばれる</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentList_ふりがな検索した場合_一致する受講生リストが返ること()
      throws Exception {

    // when: service.getStudentList のモック化
    when(service.getStudentList(furigana, false, false)).thenReturn(List.of(detailDto));

    // then: MockMvc で GETリクエストを送信
    mockMvc.perform(get("/api/students")
            .param("furigana", furigana)
            .param("includeDeleted", "false")
            .param("deletedOnly", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].student.fullName").value(fullName))
        .andExpect(jsonPath("$[0].courses[0].courseName").value(courseName));

    // serviceが呼ばれたか検証
    verify(service).getStudentList(furigana, false, false);
  }

  /**
   * 論理削除を含めて検索した場合に未削除・削除済の両方が返却されることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students}
   * <br>Params: {@code includeDeleted=true}
   * <br>Status: {@code 200 OK}
   * <p>Given:
   * <ul>
   *   <li>service.getStudentList(null, true, false) が2件（未削除1・削除済1）を返す</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvcでGET（includeDeleted=true）を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>配列長が2である</li>
   *   <li>各要素の deleted フラグとコース配列の内容が期待通りである</li>
   *   <li>service.getStudentList が期待する引数で1回呼ばれる</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentList_論理削除を含めた検索をした場合_一致する受講生リストが返ること()
      throws Exception {

    when(service.getStudentList(null, true, false)).thenReturn(List.of(detailDto1, detailDto2));

    mockMvc.perform(get("/api/students").param("includeDeleted", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].student.fullName").value("山田 太郎"))
        .andExpect(jsonPath("$[0].student.deleted").value(false))
        .andExpect(jsonPath("$[0].courses.length()").value(1))
        .andExpect(jsonPath("$[0].courses[0].courseName").value(courseName))
        .andExpect(jsonPath("$[1].student.fullName").value("削除済 太郎"))
        .andExpect(jsonPath("$[1].student.deleted").value(true))
        .andExpect(jsonPath("$[1].courses.length()").value(2))
        .andExpect(jsonPath("$[1].courses[0].courseName").value(courseDto1.getCourseName()))
        .andExpect(jsonPath("$[1].courses[1].courseName").value(courseDto2.getCourseName()));

    verify(service).getStudentList(null, true, false);
  }

  /**
   * 論理削除のみで検索した場合に削除済の受講生のみが返却されることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students}
   * <br>Params: {@code deletedOnly=true}
   * <br>Status: {@code 200 OK}
   * <p>Given:
   * <ul>
   *   <li>service.getStudentList(null, false, true) が削除済1件を返す</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvcでGET（deletedOnly=true）を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>配列長が1である</li>
   *   <li>要素の deleted フラグが true であり、コース配列が期待通りである</li>
   *   <li>service.getStudentList が期待する引数で1回呼ばれる</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentList_論理削除のみ検索した場合_削除済の受講生リストのみが返ること()
      throws Exception {

    when(service.getStudentList(null, false, true)).thenReturn(List.of(detailDto2));

    mockMvc.perform(get("/api/students").param("deletedOnly", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].student.fullName").value("削除済 太郎"))
        .andExpect(jsonPath("$[0].student.deleted").value(true))
        .andExpect(jsonPath("$[0].courses.length()").value(2))
        .andExpect(jsonPath("$[0].courses[0].courseName").value(courseDto1.getCourseName()))
        .andExpect(jsonPath("$[0].courses[1].courseName").value(courseDto2.getCourseName()));

    verify(service).getStudentList(null, false, true);
  }

  /**
   * 新規受講生を登録した際に、ステータス201と登録済みの受講生情報（Dtoレスポンス）が返却されることを検証します。
   *
   * <p>主な検証内容：
   * <ul>
   *   <li>HTTPステータスコードが 201 Created であること</li>
   *   <li>レスポンスボディに登録された受講生情報（StudentDetailDto）が正しく含まれていること</li>
   *   <li>受講生の基本情報およびコース情報が期待通りであること</li>
   * </ul>
   *
   * <p>対象エンドポイント：POST /api/students
   */
  @Test
  public void registerStudent_新規受講生登録時_ステータス201と登録済Dtoレスポンスが返ること()
      throws Exception {

    String body = """
        {
          "student": {
            "fullName": "テスト　花子",
            "furigana": "てすと　はなこ",
            "nickname": "ハナちゃん",
            "email": "test@example.com",
            "location": "大阪",
            "age": 30,
            "gender": "FEMALE",
            "remarks": "コース追加予定",
            "deleted": false
          },
          "courses": [
            {
              "courseName": "Javaコース",
              "startDate": "2024-03-01",
              "endDate": "2024-09-30"
            }
          ]
        }
        """;

    // ★ courses は startDate など必須が入っている方を使う
    var req = TestRequests.validRegistrationRequest();
    req.setStudent(studentDto);
    req.setCourses(List.of(courseDto1));

    // 変換と登録のスタブ（既存メソッドのみ）
    when(converter.toEntity(any(StudentDto.class))).thenReturn(student);

    // コントローラ内では toEntity(studentDto) で作られた student から
    // getStudentId() が渡される想定。厳密に合わせるなら eq(studentId)、
    // ゆるく通すなら any() でも可。
    when(converter.toEntityList(anyList(), any())).thenReturn(courses);

    doNothing().when(service).registerStudent(any(Student.class), anyList());

    // レスポンス DTO（toDetailDto の戻り値）
    StudentDto outStudent = new StudentDto();
    outStudent.setStudentId(base64Id);
    outStudent.setFullName(fullName);
    outStudent.setFurigana(furigana);
    outStudent.setNickname(nickname);
    outStudent.setEmail(email);
    outStudent.setLocation(location);
    outStudent.setAge(age);
    outStudent.setGender("FEMALE");
    outStudent.setRemarks(remarks);
    outStudent.setDeleted(false);

    StudentCourseDto outCourse = new StudentCourseDto();
    outCourse.setCourseName(courseName);
    outCourse.setStartDate(LocalDate.of(2025, 4, 1));
    outCourse.setEndDate(null);

    when(converter.toDetailDto(any(Student.class), anyList()))
        .thenReturn(new StudentDetailDto(outStudent, List.of(outCourse)));

    if (DEBUG) {
      StudentRegistrationRequest debug =
          objectMapper.readValue(body, StudentRegistrationRequest.class);

      System.out.println("DEBUG student=" + debug.getStudent());
      System.out.println("DEBUG courses=" + debug.getCourses());

      assertThat(debug.getStudent()).isNotNull();
      assertThat(debug.getCourses()).isNotNull();
    }

    mockMvc.perform(post("/api/students")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(body))
        .andDo(maybePrint())  // ← 無条件の .andDo(print()) を削除して、条件付きへ
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.student.studentId").value(base64Id))
        .andExpect(jsonPath("$.student.fullName").value(fullName))
        .andExpect(jsonPath("$.student.furigana").value(furigana))
        .andExpect(jsonPath("$.student.nickname").value(nickname))
        .andExpect(jsonPath("$.student.email").value(email))
        .andExpect(jsonPath("$.student.location").value(location))
        .andExpect(jsonPath("$.student.age").value(age))
        .andExpect(jsonPath("$.student.gender").value("FEMALE"))
        .andExpect(jsonPath("$.student.remarks").value(remarks))
        .andExpect(jsonPath("$.student.deleted").value(false))
        .andExpect(jsonPath("$.courses[0].courseName").value(courseName))
        .andExpect(jsonPath("$.courses[0].startDate").value("2025-04-01"))
        .andExpect(jsonPath("$.courses[0].endDate").isEmpty());
    // ← 末尾の andDo(System.out.println(...)) も削除。maybePrint() 内で実施するので不要

    // （任意の検証）
    verify(converter).toEntity(any(StudentDto.class));
    verify(converter).toEntityList(anyList(), any());
    verify(service).registerStudent(any(Student.class), anyList());
    verify(converter).toDetailDto(any(Student.class), anyList());
  }

  /**
   * 受講生情報を更新した際に 200 と更新済みの詳細DTOが返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 200 OK}
   * <p>Given:
   * <ul>
   *   <li>有効な {@link StudentRegistrationRequest}（student + 1件のcourse）</li>
   *   <li>{@code converter.toEntity}, {@code converter.toEntityList}, {@code service.updateStudentWithCourses} の正常スタブ</li>
   *   <li>{@code converter.toDetailDto(entity, courses, base64Id)} が期待DTOを返す</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvcでPUTを実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>HTTP 200 が返る</li>
   *   <li>student配下の各フィールドが期待どおり</li>
   *   <li>必要なモックが期待引数で呼ばれる</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_受講生情報を更新した時_ステータス200と更新済Dtoレスポンスが返ること()
      throws Exception {

    var req = new StudentRegistrationRequest();
    req.setStudent(studentDto);
    req.setCourses(List.of(courseDto));

    // コントローラの戻りを固定：DTOを直接返す設計なら toDetailDto のみでOK
    StudentDetailDto expected = new StudentDetailDto(studentDto, List.of(courseDto));

    when(converter.toDetailDto(any(Student.class), anyList(), eq(base64Id)// コントローラがレスポンスIDに使うBase64
    )).thenReturn(expected);
    when(converter.toEntity(studentDto)).thenReturn(student);
    when(converter.toEntityList(eq(List.of(courseDto)),
        argThat(arr -> Arrays.equals(arr, studentId)))).thenReturn(courses);
    // （任意・安全策）サービスが戻り値を返す設計なら
    when(service.updateStudentWithCourses(same(student), anyList()))
        .thenReturn(student);

    // Act & Assert
    mockMvc.perform(put("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.student.studentId").value(base64Id))
        .andExpect(jsonPath("$.student.fullName").value(fullName))
        .andExpect(jsonPath("$.student.furigana").value(furigana))
        .andExpect(jsonPath("$.student.nickname").value(nickname))
        .andExpect(jsonPath("$.student.email").value(email))
        .andExpect(jsonPath("$.student.location").value(location))
        .andExpect(jsonPath("$.student.age").value(age))
        .andExpect(jsonPath("$.student.gender").value(gender))
        .andExpect(jsonPath("$.student.remarks").value(remarks))
        .andExpect(jsonPath("$.student.deleted").value(false));

    // （任意）呼び出し検証
    verify(converter).decodeUuidOrThrow(base64Id);
    verify(converter).toEntity(studentDto);
    verify(converter).toEntityList(anyList(), argThat(arr -> Arrays.equals(arr, studentId)));
    // verify も3引数に
    verify(converter).encodeBase64(argThat(arr -> Arrays.equals(arr, studentId))); // 追加
    verify(converter).toDetailDto(any(Student.class), same(courses), eq(base64Id));
    verifyNoMoreInteractions(converter);
  }

  /**
   * 部分更新（置換モード）で基本情報とコース差分を反映し、200が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 200 OK}
   * <p>Given:
   * <ul>
   *   <li>既存受講生の取得・マージ・部分更新が正常</li>
   *   <li>更新後の再取得・コース再検索・DTO化が正常</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>HTTP 200 が返る</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void partialUpdateStudent_受講生情報を部分更新した場合_200を返すこと() throws Exception {

    var req = new StudentRegistrationRequest();
    req.setStudent(new StudentDto());
    req.setCourses(List.of(courseDto));
    req.setAppendCourses(false);

    Student existing = new Student();
    Student merged = new Student();
    List<StudentCourse> newCourses = List.of(new StudentCourse());
    Student updated = new Student();
    List<StudentCourse> updatedCourses = List.of(new StudentCourse());
    StudentDetailDto out = new StudentDetailDto();

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    when(service.findStudentById(studentId)).thenReturn(existing);
    when(converter.toEntity(any(StudentDto.class))).thenReturn(merged);
    doNothing().when(converter).mergeStudent(existing, merged);
    when(converter.toEntityList(List.of(courseDto), studentId)).thenReturn(newCourses);
    doNothing().when(service).partialUpdateStudent(existing, newCourses);
    when(service.findStudentById(studentId)).thenReturn(updated);
    when(service.searchCoursesByStudentId(studentId)).thenReturn(updatedCourses);
    when(converter.toDetailDto(updated, updatedCourses)).thenReturn(out);

    mockMvc.perform(patch("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(req)))
        .andExpect(status().isOk());
  }

  /**
   * 部分更新で courses が空でも基本情報のみ更新され 200 が返ることを検証します（置換モード）。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 200 OK}
   * <p>Given:
   * <ul>
   *   <li>courses=[] を送信</li>
   *   <li>既存受講生の取得とマージ、情報のみ更新が正常</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>HTTP 200 が返り、氏名等が更新される</li>
   *   <li>{@code service.updateStudentInfoOnly(existing)} が呼ばれる</li>
   *   <li>{@code toEntityList} は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void partialUpdateStudent_coursesがnullや空でも基本情報だけ更新され_200を返すこと()
      throws Exception {
    // 入力（courses: 空配列）
    String body = """
        {
          "student": { "name": "新しい名前" },
          "courses": []
        }
        """;

    // --- Mock 準備 ---
    when(converter.decodeBase64(base64Id)).thenReturn(studentId);

    Student existing = new Student();
    when(service.findStudentById(studentId)).thenReturn(existing);

    // student のマージ
    Student merged = new Student();
    when(converter.toEntity(studentDto)).thenReturn(merged);
    // JSON直書きなので、studentDto を使わないならここは不要。使うなら body を json(...) で作る
    doNothing().when(converter).mergeStudent(existing, merged);

    // courses 変換は空リストを返す
    when(converter.toEntityList(Collections.emptyList(), studentId)).thenReturn(
        Collections.emptyList());

    // コース再取得は空を返す想定
    when(service.searchCoursesByStudentId(studentId)).thenReturn(Collections.emptyList());

    // findStudentById は 2回呼ばれるので、1回目: existing, 2回目: updated を返す
    Student updated = new Student();
    when(service.findStudentById(studentId)).thenReturn(existing, updated);

    // レスポンスDTOをスタブ（name に "新しい名前" が入るように）
    StudentDto respStudent = new StudentDto();
    respStudent.setFullName("新しい名前"); // ※JSONでは $.student.name を見ている前提
    StudentDetailDto resp = new StudentDetailDto(respStudent, Collections.emptyList());
    when(converter.toDetailDto(updated, Collections.emptyList())).thenReturn(resp);

    // --- 実行 & 検証 ---
    mockMvc.perform(patch("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.student.fullName").value("新しい名前"));

    // 期待：サービスは動いている（NoInteractions ではない）
    verify(converter).decodeBase64(base64Id);
    verify(service, times(2)).findStudentById(studentId);
    verify(converter, never()).toEntityList(anyList(), eq(studentId));

    // 置換モードなので updateStudentInfoOnly は呼ばれる（空でもOK、Service側が無視）
    verify(service).updateStudentInfoOnly(existing);

    // updateStudentInfoOnlyで「既存コースの再取得される
    verify(service).searchCoursesByStudentId(studentId);
  }

  /**
   * 論理削除が成功した場合に 204 No Content が返ることを検証します。
   * <p>
   * Endpoint: {@code DELETE /api/students/{studentId}}
   * <br>Status: {@code 204 NO_CONTENT}
   * Then:
   * <ul>
   *   <li>レスポンスボディが空である</li>
   *   <li>{@code service.softDeleteStudent(studentId)} が1回呼ばれる</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void deleteStudent_論理削除に成功した場合_204_No_Contentを返すこと() throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);

    mockMvc.perform(delete("/api/students/{studentId}", base64Id))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(converter).decodeBase64(base64Id);
    verify(service).softDeleteStudent(eq(studentId));
    verifyNoMoreInteractions(service);
  }

  /**
   * 論理削除済みの受講生を復元した場合に 204 No Content が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}/restore}
   * <br>Status: {@code 204 NO_CONTENT}
   * Then:
   * <ul>
   *   <li>レスポンスボディが空である</li>
   *   <li>{@code service.restoreStudent(studentId)} が1回呼ばれる</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void restoreStudent_復元に成功したら204を返すこと() throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);

    mockMvc.perform(patch("/api/students/{studentId}/restore", base64Id))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(converter).decodeBase64(base64Id);
    verify(service).restoreStudent(eq(studentId));
    verifyNoMoreInteractions(service);
  }
}
