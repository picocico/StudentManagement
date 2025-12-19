package raisetech.student.management.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
   *
   * <p>Endpoint: {@code GET /api/students/{studentId}}<br>
   * Status: {@code 200 OK}
   *
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeUuidStringOrThrow(studentById)} が
   *   16 バイト長の受講生ID（UUIDバイト配列）を返す
   *   <li>{@code service.findStudentById(studentId)} が既存の受講生エンティティを返す
   *   <li>{@code service.searchCoursesByStudentId(studentId)} が 2 件の受講コースを返す
   *   <li>{@code converter.toDetailDto(student, courses)} が期待される {@link StudentDetailDto} を返す
   * </ul>
   *
   * <p>When:
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/{studentId}} を実行する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 200 が返る
   *   <li>レスポンスの {@code $.student.*} および {@code $.courses[*]} の各フィールドが期待値どおりである
   *   <li>2件目のコースの {@code endDate} が {@code null} として返却されることを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void getStudentDetail_受講生ID検索した場合_一致する受講生詳細が返ること()
      throws Exception {

    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);
    when(service.findStudentById(studentId)).thenReturn(student);
    when(service.searchCoursesByStudentId(studentId)).thenReturn(courses);
    when(converter.toDetailDto(student, courses))
        .thenReturn(new StudentDetailDto(studentDto, List.of(courseDto1, courseDto2)));

    mockMvc
        .perform(get("/api/students/{studentId}", studentById))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.student.studentId").value(studentById))
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
   *
   * <p>Endpoint: {@code GET /api/students/{studentId}} <br>
   * Status: {@code 200 OK}
   *
   * <p>Given:
   *
   * <ul>
   *   <li>{@code converter.decodeUuidStringToBytesOrThrow(studentById)} が
   *   16 バイト長の受講生ID（UUIDバイト配列）を返す
   *   <li>{@code service.findStudentById(studentId)} が既存の受講生を返す
   *   <li>{@code service.searchCoursesByStudentId(studentId)} が空リストを返す
   *   <li>{@code converter.toDetailDto(student, emptyList)} が期待される {@link StudentDetailDto} を返す
   * </ul>
   *
   * <p>When:
   *
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/{studentId}} を実行する
   * </ul>
   *
   * <p>Then:
   *
   * <ul>
   *   <li>HTTP 200 が返る
   *   <li>{@code $.courses.length()==0} を検証する
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentDetail_受講コースが存在しない場合_空のコースリストが返ること()
      throws Exception {

    // given
    String idStr = studentById;   // 既にテストクラスで用意している UUID 文字列
    UUID idUuid = studentId;      // 既に @BeforeEach で UUID.fromString している想定

    // 1) String -> UUID デコード
    when(converter.decodeUuidStringOrThrow(idStr)).thenReturn(idUuid);

    // 2) Student は存在するが、コースは0件
    Student student = new Student();
    student.setStudentId(idUuid);
    student.setFullName("山田 太郎");

    when(service.findStudentById(idUuid)).thenReturn(student);
    when(service.searchCoursesByStudentId(idUuid)).thenReturn(List.of());

    // 3) Converter が返す DTO を明示的にスタブする
    StudentDto studentDto =
        new StudentDto(idStr, "山田 太郎", "やまだ たろう", "タロウ",
            "taro@example.com", "Osaka", 25, "Male", "備考", false);

    StudentDetailDto detailDto = new StudentDetailDto(studentDto, List.of());

    when(converter.toDetailDto(student, List.of()))
        .thenReturn(detailDto);

    // when & then
    mockMvc
        .perform(get("/api/students/{studentId}", idStr))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.student.fullName").value("山田 太郎"))
        .andExpect(jsonPath("$.courses").isArray())
        .andExpect(jsonPath("$.courses").isEmpty());

    verify(converter).decodeUuidStringOrThrow(idStr);
    verify(service).findStudentById(idUuid);
    verify(service).searchCoursesByStudentId(idUuid);
    verify(converter).toDetailDto(student, List.of());
  }

  /**
   * ふりがなで検索した場合に一致する受講生リストが返却されることを検証します。
   *
   * <p>Endpoint: {@code GET /api/students}<br>
   * Params:
   * <ul>
   *   <li>{@code furigana={指定値}}
   *   <li>{@code includeDeleted=false}
   *   <li>{@code deletedOnly=false}
   * </ul>
   * Status: {@code 200 OK}
   *
   * <p>Given:
   * <ul>
   *   <li>{@code service.getStudentList(furigana, false, false)} が 1 件の {@link StudentDetailDto} を返す
   * </ul>
   *
   * <p>When:
   * <ul>
   *   <li>MockMvc でクエリパラメータ付き {@code GET /api/students} を実行する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>レスポンス配列長が 1 である
   *   <li>先頭要素の氏名およびコース名が期待どおりである
   *   <li>{@code service.getStudentList(furigana, false, false)} が 1 回呼び出される
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void getStudentList_ふりがな検索した場合_一致する受講生リストが返ること()
      throws Exception {

    // when: service.getStudentList のモック化
    when(service.getStudentList(furigana, false, false)).thenReturn(List.of(detailDto));

    // then: MockMvc で GETリクエストを送信
    mockMvc
        .perform(
            get("/api/students")
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
   * 論理削除を含めて検索した場合に、未削除・削除済みの両方が返却されることを検証します。
   *
   * <p>Endpoint: {@code GET /api/students}<br>
   * Params:
   * <ul>
   *   <li>{@code includeDeleted=true}
   *   <li>{@code deletedOnly} は指定なし（false）
   * </ul>
   * Status: {@code 200 OK}
   *
   * <p>Given:
   * <ul>
   *   <li>{@code service.getStudentList(null, true, false)} が 2 件（未削除 1・削除済 1）の {@link StudentDetailDto} を返す
   * </ul>
   *
   * <p>When:
   * <ul>
   *   <li>MockMvc で {@code includeDeleted=true} を付与して {@code GET /api/students} を実行する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>レスポンス配列長が 2 である
   *   <li>1件目は deleted=false、2件目は deleted=true である
   *   <li>各要素のコース配列の件数およびコース名が期待どおりである
   *   <li>{@code service.getStudentList(null, true, false)} が 1 回呼び出される
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void getStudentList_論理削除を含めた検索をした場合_一致する受講生リストが返ること()
      throws Exception {

    when(service.getStudentList(null, true, false)).thenReturn(List.of(detailDto1, detailDto2));

    mockMvc
        .perform(get("/api/students").param("includeDeleted", "true"))
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
   * 論理削除のみで検索した場合に、削除済みの受講生のみが返却されることを検証します。
   *
   * <p>Endpoint: {@code GET /api/students}<br>
   * Params:
   * <ul>
   *   <li>{@code deletedOnly=true}
   *   <li>{@code includeDeleted} は指定なし（false）
   * </ul>
   * Status: {@code 200 OK}
   *
   * <p>Given:
   * <ul>
   *   <li>{@code service.getStudentList(null, false, true)} が削除済み 1 件の {@link StudentDetailDto} を返す
   * </ul>
   *
   * <p>When:
   * <ul>
   *   <li>MockMvc で {@code deletedOnly=true} を付与して {@code GET /api/students} を実行する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>レスポンス配列長が 1 である
   *   <li>要素の {@code deleted} フラグが {@code true} である
   *   <li>コース配列の件数およびコース名が期待どおりである
   *   <li>{@code service.getStudentList(null, false, true)} が 1 回呼び出される
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void getStudentList_論理削除のみ検索した場合_削除済の受講生リストのみが返ること()
      throws Exception {

    when(service.getStudentList(null, false, true)).thenReturn(List.of(detailDto2));

    mockMvc
        .perform(get("/api/students").param("deletedOnly", "true"))
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
   * 新規受講生を登録した際に、ステータス 201 と登録済みの受講生詳細 DTO が返却されることを検証します。
   *
   * <p>Endpoint: {@code POST /api/students}<br>
   * Status: {@code 201 CREATED}
   *
   * <p>Given:
   * <ul>
   *   <li>リクエストボディに有効な {@link StudentRegistrationRequest}（student + 1 件の course）が指定されている
   *   <li>{@code converter.toEntity(studentDto)} が受講生エンティティを返す
   *   <li>{@code converter.toEntityList(courses, studentId)} が受講コースエンティティのリストを返す
   *   <li>{@code service.registerStudent(student, courses)} が正常終了する
   *   <li>{@code converter.toDetailDto(student, courses)} が登録結果の {@link StudentDetailDto} を返す
   * </ul>
   *
   * <p>When:
   * <ul>
   *   <li>MockMvc で {@code POST /api/students} を JSON ボディ付きで実行する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 201 が返る
   *   <li>レスポンスの受講生基本情報およびコース情報が期待どおりである
   *   <li>{@code converter.toEntity}, {@code converter.toEntityList}, {@code service.registerStudent},
   *       {@code converter.toDetailDto} が呼び出されることを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void registerStudent_新規受講生登録時_ステータス201と登録済Dtoレスポンスが返ること()
      throws Exception {

    String body =
        """
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
    outStudent.setStudentId(studentById);
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
      var root = objectMapper.readTree(body);
      assertThat(root.path("student").isMissingNode()).isFalse();
      assertThat(root.path("courses").isArray()).isTrue();
    }

    mockMvc
        .perform(
            post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.student.studentId").value(studentById))
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
   * 受講生情報を更新した際に、200 と更新済みの詳細 DTO が返ることを検証します。
   *
   * <p>Endpoint: {@code PUT /api/students/{studentId}}<br>
   * Status: {@code 200 OK}
   *
   * <p>Given:
   * <ul>
   *   <li>有効な {@link StudentRegistrationRequest}（student + 1 件の course）が渡される
   *   <li>{@code converter.toEntity(studentDto)} が受講生エンティティを返す
   *   <li>{@code converter.toEntityList(courses, studentId)} が受講コースエンティティを返す
   *   <li>{@code service.updateStudentWithCourses(student, courses)} が正常終了する
   *   <li>{@code converter.encodeUuidString(studentIdBytes)} と
   *       3 引数版{@code converter.toDetailDto(updated, courses, studentIdString)} が
   *       レスポンス用 DTO を生成する
   * </ul>
   *
   * <p>When:
   * <ul>
   *   <li>MockMvc で {@code PUT /api/students/{studentId}} を JSON ボディ付きで実行する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 200 が返る
   *   <li>レスポンスの {@code $.student.*} が期待どおりである
   *   <li>{@code converter.decodeUuidStringToBytesOrThrow(studentById)}
   *   　　や各変換メソッドが期待どおり呼び出される
   *   <li>UUID ⇔ byte[] の ID 変換と 3 引数版 {@code converter.toDetailDto(...)}
   *   　　を使ってレスポンス DTO が組み立てられることを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void updateStudent_受講生情報を更新した時_ステータス200と更新済Dtoレスポンスが返ること()
      throws Exception {

    // given
    String idStr = studentById;   // "123e4567-e89b-..."
    UUID idUuid = studentId;      // @BeforeEach で UUID.fromString している想定

    // リクエストDTO
    StudentDto bodyStudentDto = new StudentDto(
        idStr,
        "テスト　花子",
        "てすと　はなこ",
        "ハナちゃん",
        "test@example.com",
        "大阪",
        30,
        "Female",
        "コース追加予定",
        false
    );

    StudentCourseDto bodyCourseDto =
        new StudentCourseDto(null, "Javaコース", null, null);

    StudentRegistrationRequest req = new StudentRegistrationRequest();
    req.setStudent(bodyStudentDto);
    req.setCourses(List.of(bodyCourseDto));

    // Entity 側
    Student entityBefore = new Student();
    entityBefore.setStudentId(idUuid);
    entityBefore.setFullName("テスト　花子");
    // …必要なら他のフィールドも

    List<StudentCourse> entityCourses = List.of(new StudentCourse());
    entityCourses.get(0).setStudentId(idUuid);

    Student updated = new Student();
    updated.setStudentId(idUuid);
    updated.setFullName("テスト　花子");

    StudentDetailDto detailDto =
        new StudentDetailDto(bodyStudentDto, List.of(
            new StudentCourseDto("some-uuid", "Javaコース", null, null)));

    // スタブ
    when(converter.decodeUuidStringOrThrow(idStr)).thenReturn(idUuid);
    when(converter.toEntity(bodyStudentDto)).thenReturn(entityBefore);
    when(converter.toEntityList(req.getCourses(), idUuid)).thenReturn(entityCourses);
    when(service.updateStudentWithCourses(entityBefore, entityCourses))
        .thenReturn(updated);
    // ★ ここだけで十分（パスで受け取った UUID 文字列をそのまま第三引数へ）
    when(converter.toDetailDto(updated, entityCourses, idStr))
        .thenReturn(detailDto);

    // when & then
    mockMvc
        .perform(
            put("/api/students/{studentId}", idStr)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.student.studentId").value(idStr))
        .andExpect(jsonPath("$.student.fullName").value("テスト　花子"));

    // verify（encodeUuidString はもう期待しない）
    verify(converter).decodeUuidStringOrThrow(idStr);
    verify(converter).toEntity(bodyStudentDto);
    verify(converter).toEntityList(req.getCourses(), idUuid);
    verify(service).updateStudentWithCourses(entityBefore, entityCourses);
    verify(converter).toDetailDto(updated, entityCourses, idStr);
    verifyNoMoreInteractions(converter, service);
  }

  /**
   * 部分更新（置換モード）で基本情報とコース差分を反映し、200 が返ることを検証します。
   *
   * <p>Endpoint: {@code PATCH /api/students/{studentId}}<br>
   * Status: {@code 200 OK}
   *
   * <p>Given:
   * <ul>
   *   <li>{@code appendCourses=false} として部分更新をリクエストする（置換モード）
   *   <li>既存受講生の取得（{@code service.findStudentById}）、エンティティ変換、マージ処理が正常に行われる
   *   <li>{@code service.partialUpdateStudent(existing, newCourses)} が正常終了する
   *   <li>更新後の再取得・コース再検索・DTO 化が正常に行われる
   * </ul>
   *
   * <p>When:
   * <ul>
   *   <li>MockMvc で {@code PATCH /api/students/{studentId}} を JSON ボディ付きで実行する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 200 が返る
   *   <li>DTO 変換まで正常に完了し、エラーが発生しないことを検証する
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
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

    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);
    when(service.findStudentById(studentId)).thenReturn(existing);
    when(converter.toEntity(any(StudentDto.class))).thenReturn(merged);
    doNothing().when(converter).mergeStudent(existing, merged);
    when(converter.toEntityList(List.of(courseDto), studentId)).thenReturn(newCourses);
    doNothing().when(service).partialUpdateStudent(existing, newCourses);
    when(service.findStudentById(studentId)).thenReturn(updated);
    when(service.searchCoursesByStudentId(studentId)).thenReturn(updatedCourses);
    when(converter.toDetailDto(updated, updatedCourses)).thenReturn(out);

    mockMvc
        .perform(
            patch("/api/students/{studentId}", studentById)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
        .andExpect(status().isOk());
  }

  /**
   * 部分更新で {@code courses} が空でも、基本情報のみ更新され 200 が返ること（置換モード）を検証します。
   *
   * <p>Endpoint: {@code PATCH /api/students/{studentId}}<br>
   * Status: {@code 200 OK}
   *
   * <p>Given:
   * <ul>
   *   <li>リクエストボディで {@code courses=[]} を指定する
   *   <li>既存受講生の取得とマージ処理が正常に行われる
   *   <li>{@code service.updateStudentInfoOnly(existing)} が呼び出される
   *   <li>コースは変更されず、再取得結果は空リストになる
   * </ul>
   *
   * <p>When:
   * <ul>
   *   <li>MockMvc で {@code PATCH /api/students/{studentId}} を JSON ボディ付きで実行する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 200 が返る
   *   <li>レスポンスの {@code $.student.fullName} が {@code "新しい名前"} に更新されている
   *   <li>{@code converter.decodeUuidStringToBytesOrThrow(studentById)} が呼び出される
   *   <li>{@code converter.toEntityList(...)} は呼び出されない
   *   <li>{@code service.updateStudentInfoOnly(existing)} および {@code service.searchCoursesByStudentId(studentId)} が呼び出される
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void partialUpdateStudent_coursesがnullや空でも基本情報だけ更新され_200を返すこと()
      throws Exception {
    // 入力（courses: 空配列）
    String body =
        """
            {
              "student": { "fullName": "新しい名前" },
              "courses": []
            }
            """;

    // --- Mock 準備 ---
    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);

    Student existing = new Student();
    when(service.findStudentById(studentId)).thenReturn(existing);

    // student のマージ
    Student merged = new Student();
    when(converter.toEntity(studentDto)).thenReturn(merged);
    // JSON直書きなので、studentDto を使わないならここは不要。使うなら body を json(...) で作る
    doNothing().when(converter).mergeStudent(existing, merged);

    // courses 変換は空リストを返す
    when(converter.toEntityList(Collections.emptyList(), studentId))
        .thenReturn(Collections.emptyList());

    // コース再取得は空を返す想定
    when(service.searchCoursesByStudentId(studentId)).thenReturn(Collections.emptyList());

    // findStudentById は 2回呼ばれるので、1回目: existing, 2回目: updated を返す
    Student updated = new Student();
    when(service.findStudentById(studentId)).thenReturn(existing, updated);

    // レスポンスDTOをスタブ（name に "新しい名前" が入るように）
    StudentDto respStudent = new StudentDto();
    respStudent.setFullName("新しい名前"); // ※JSONでは $.student.name を見ている前提
    StudentDetailDto resp = new StudentDetailDto(respStudent, Collections.emptyList());
    when(converter.toDetailDto(updated, Collections.emptyList(), studentById)).thenReturn(resp);

    // --- 実行 & 検証 ---
    mockMvc
        .perform(
            patch("/api/students/{studentId}", studentById)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.student.fullName").value("新しい名前"));

    // 期待：サービスは動いている（NoInteractions ではない）
    verify(converter).decodeUuidStringOrThrow(studentById);
    verify(service, times(2)).findStudentById(studentId);
    verify(converter, never()).toEntityList(anyList(), eq(studentId));

    // 置換モードなので updateStudentInfoOnly は呼ばれる（空でもOK、Service側が無視）
    verify(service).updateStudentInfoOnly(existing);

    // updateStudentInfoOnlyで「既存コースの再取得される
    verify(service).searchCoursesByStudentId(studentId);
  }

  /**
   * 論理削除が成功した場合に、204 No Content が返ることを検証します。
   *
   * <p>Endpoint: {@code DELETE /api/students/{studentId}}<br>
   * Status: {@code 204 NO_CONTENT}
   *
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeUuidStringToBytesOrThrow(studentById)} により受講生IDがデコードされる
   *   <li>{@code service.softDeleteStudent(studentId)} が正常終了する
   * </ul>
   *
   * <p>When:
   * <ul>
   *   <li>MockMvc で {@code DELETE /api/students/{studentId}} を実行する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 204 が返る
   *   <li>レスポンスボディが空文字である
   *   <li>{@code service.softDeleteStudent(studentId)} が 1 回だけ呼び出され、それ以外のサービス呼び出しが行われない
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void deleteStudent_論理削除に成功した場合_204_No_Contentを返すこと() throws Exception {

    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);

    mockMvc
        .perform(delete("/api/students/{studentId}", studentById))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(converter).decodeUuidStringOrThrow(studentById);
    verify(service).softDeleteStudent(eq(studentId));
    verifyNoMoreInteractions(service);
  }

  /**
   * 論理削除済みの受講生を復元した場合に、204 No Content が返ることを検証します。
   *
   * <p>Endpoint: {@code PATCH /api/students/{studentId}/restore}<br>
   * Status: {@code 204 NO_CONTENT}
   *
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeUuidStringToBytesOrThrow(studentById)} により受講生IDがデコードされる
   *   <li>{@code service.restoreStudent(studentId)} が正常終了する
   * </ul>
   *
   * <p>When:
   * <ul>
   *   <li>MockMvc で {@code PATCH /api/students/{studentId}/restore} を実行する
   * </ul>
   *
   * <p>Then:
   * <ul>
   *   <li>HTTP ステータス 204 が返る
   *   <li>レスポンスボディが空文字である
   *   <li>{@code service.restoreStudent(studentId)} が 1 回だけ呼び出され、それ以外のサービス呼び出しが行われない
   * </ul>
   *
   * @throws Exception MockMvc 実行時の例外
   */
  @Test
  public void restoreStudent_復元に成功したら204を返すこと() throws Exception {

    when(converter.decodeUuidStringOrThrow(studentById)).thenReturn(studentId);

    mockMvc
        .perform(patch("/api/students/{studentId}/restore", studentById))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(converter).decodeUuidStringOrThrow(studentById);
    verify(service).restoreStudent(eq(studentId));
    verifyNoMoreInteractions(service);
  }
}
