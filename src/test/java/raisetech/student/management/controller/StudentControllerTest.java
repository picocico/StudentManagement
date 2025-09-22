package raisetech.student.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.BDDMockito.Then;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.web.bind.MissingServletRequestParameterException;
import raisetech.student.management.config.TestMockConfig;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentCourseDto;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentDto;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.exception.GlobalExceptionHandler;
import raisetech.student.management.exception.InvalidIdFormatException;
import raisetech.student.management.exception.ResourceNotFoundException;
import raisetech.student.management.service.StudentService;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * このクラスは、{@link StudentController} の各RESTエンドポイントの動作を検証するテストクラスです。
 * <p>
 * Spring MVC の {@code MockMvc} を使用して、各RESTエンドポイントに対する 正常系および異常系のリクエストに対するレスポンス内容を検証します。
 * </p>
 *
 * <p>テスト対象：</p>
 * <ul>
 *   <li>各APIエンドポイントの正常なレスポンス内容</li>
 *   <li>バリデーション違反などによる入力エラー時のレスポンス</li>
 *   <li>リソースが存在しない場合の例外応答（例：{@code ResourceNotFoundException}）</li>
 *   <li>パラメータ形式の不正によるエラー（例：{@code MethodArgumentTypeMismatchException}）</li>
 * </ul>
 *
 * <p>使用アノテーション：</p>
 * <ul>
 *   <li>{@code @WebMvcTest(StudentController.class)}：Web層（Controller）のみに限定したテスト</li>
 *   <li>{@code @Import(GlobalExceptionHandler.class)}：例外ハンドラーを有効化</li>
 *   <li>{@code @MockBean}：依存する {@code StudentService} や {@code StudentConverter} をモック化</li>
 * </ul>
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = StudentController.class)
@Import({GlobalExceptionHandler.class, TestMockConfig.class})
@ImportAutoConfiguration(exclude = {GsonAutoConfiguration.class})
class StudentControllerTest {

  /**
   * Controller のテストにおいて、HTTPリクエストおよびレスポンスを模倣するためのテスト用オブジェクト。
   * <p>
   * {@code @WebMvcTest} などでSpring MVCの振る舞いを検証する際に使用される。
   */
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  /**
   * モック化された {@link StudentService}。
   * <p>
   * コントローラの依存性として注入され、サービス層のロジックをテスト対象から切り離します
   */
  @Autowired
  private StudentService service;

  /**
   * テスト対象の {@link StudentController} に注入されるモックの {@link StudentConverter}。
   * <p>
   * 実際のエンティティ・DTO変換処理は行わず、必要に応じてスタブ化された動作により、 コントローラーの単体テストを実現します。
   */
  @Autowired
  private StudentConverter converter;

  @Autowired
  private org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter rmha;

  // ←――――――――――――――――――――――――――――――――――――――――――――――
  //  UUID→16バイト→Base64 変換ユーティリティ
  // ―――――――――――――――――――――――――――――――――――――――――――→
  /**
   * UUID文字列を16バイト配列にエンコードし、URLセーフなBase64（パディング無し）に変換します。
   *
   * @param uuid ハイフン区切りのUUID文字列
   * @return Base64URL形式のID文字列
   * @throws IllegalArgumentException UUID形式が不正な場合
   */
  private static String base64FromUuid(String uuid) {
    var u = UUID.fromString(uuid);
    var bb = ByteBuffer.allocate(16);
    bb.putLong(u.getMostSignificantBits());
    bb.putLong(u.getLeastSignificantBits());
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array());
  }

  /**
   * UUID文字列を16バイトの配列に変換します。
   *
   * @param uuid ハイフン区切りのUUID文字列
   * @return 16バイトのUUID表現
   * @throws IllegalArgumentException UUID形式が不正な場合
   */
  private static byte[] bytesFromUuid(String uuid) {
    var u = UUID.fromString(uuid);
    var bb = ByteBuffer.allocate(16);
    bb.putLong(u.getMostSignificantBits());
    bb.putLong(u.getLeastSignificantBits());
    return bb.array();
  }

  // テスト共通で使う「常に16バイト」のID
  private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426655440000";

  private byte[] studentId;
  private String base64Id;

  private String fullName;
  private String furigana;
  private String nickname;
  private String email;
  private String location;
  private int age;
  private String gender;
  private String remarks;

  private String courseName;
  private String secondCourseName;

  private Student student;
  private StudentDto studentDto;

  private StudentCourse course1;
  private StudentCourse course2;
  private List<StudentCourse> courses;

  private StudentCourseDto courseDto;
  private StudentCourseDto courseDto1;
  private StudentCourseDto courseDto2;

  private StudentDetailDto detailDto;
  private StudentDetailDto detailDto1;
  private StudentDetailDto detailDto2;

  // 便利関数
  /**
   * オブジェクトをJSON文字列にシリアライズします。
   *
   * @param o シリアライズ対象オブジェクト
   * @return JSON文字列
   * @throws Exception シリアライズに失敗した場合
   */
  private String json(Object o) throws Exception {
    return objectMapper.writeValueAsString(o);
  }

  /**
   * 各テスト実行前の共通初期化を行います。
   * <p>
   * Given:
   * <ul>
   *   <li>固定のUUID/16バイトID/base64Idの生成</li>
   *   <li>受講生・コースのダミーデータおよびDTOの用意</li>
   *   <li>モックの完全リセットと、共通スタブ（decodeUuidOrThrow, encodeBase64）の再設定</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>以降の各テストが同一前提で安定して実行できる状態になる</li>
   * </ul>
   */
  @BeforeEach
  void setupAll() {
    /**
     * 各テスト実行前に {@code MockMvc} を初期化します。
     **/
    // 1) 値の初期化 16バイトIDを必ず使用
    base64Id = base64FromUuid(VALID_UUID);
    studentId = bytesFromUuid(VALID_UUID);

    fullName = "テスト　花子";
    furigana = "てすと　はなこ";
    nickname = "ハナちゃん";
    email = "test@example.com";
    location = "大阪";
    age = 30;
    gender = "Female";
    remarks = "コース追加予定";

    courseName = "Javaコース";
    secondCourseName = "AWSコース";

    student = new Student();
    student.setStudentId(studentId);
    student.setFullName(fullName);
    student.setFurigana(furigana);
    student.setNickname(nickname);
    student.setEmail(email);
    student.setLocation(location);
    student.setAge(age);
    student.setGender(gender);
    student.setRemarks(remarks);
    student.setDeleted(false);

    // 通常の受講生 DTO
    StudentDto s = new StudentDto();
    s.setStudentId(base64Id); // ← レスポンス検証用
    s.setFullName(fullName);
    s.setFurigana(furigana);
    s.setNickname(nickname);
    s.setEmail(email);
    s.setLocation(location);
    s.setAge(age);
    s.setGender(gender);
    s.setRemarks(remarks);
    s.setDeleted(false);
    studentDto = s;

    // コース（entity）
    course1 = new StudentCourse();
    course1.setStudentId(studentId);
    course1.setCourseId(new byte[16]);
    course1.setCourseName(courseName);
    course1.setStartDate(LocalDate.of(2024, 3, 1));
    course1.setEndDate(LocalDate.of(2024, 9, 30));
    course1.setCreatedAt(LocalDateTime.now());

    course2 = new StudentCourse();
    course2.setStudentId(studentId);
    course2.setCourseId(new byte[16]);
    course2.setCourseName(secondCourseName);
    course2.setStartDate(LocalDate.of(2024, 7, 1));
    course2.setEndDate(null);
    course2.setCreatedAt(LocalDateTime.now());

    courses = List.of(course1, course2);

    // コースDTO
    courseDto = new StudentCourseDto();
    courseDto.setCourseName(courseName);

    courseDto1 = new StudentCourseDto();
    courseDto1.setCourseName(courseName);
    courseDto1.setStartDate(LocalDate.of(2024, 3, 1));
    courseDto1.setEndDate(LocalDate.of(2024, 9, 30));

    courseDto2 = new StudentCourseDto();
    courseDto2.setCourseName(secondCourseName);
    courseDto2.setStartDate(LocalDate.of(2024, 7, 1));
    courseDto2.setEndDate(null);

    // 一覧系のダミー
    detailDto = new StudentDetailDto(studentDto, List.of(courseDto1, courseDto2));

    // 削除されていない受講生
    StudentDto student1 = new StudentDto();
    student1.setFullName("山田 太郎");
    student1.setDeleted(false);
    detailDto1 = new StudentDetailDto(student1, List.of(courseDto1));

    // 論理削除された受講生
    StudentDto student2 = new StudentDto();
    student2.setFullName("削除済 太郎");
    student2.setDeleted(true);
    detailDto2 = new StudentDetailDto(student2, List.of(courseDto1, courseDto2));

    // 2) 完全リセット（呼び出し履歴＋スタブの両方クリア）
    Mockito.reset(converter, service /* , 他のモック */);

    // 3) ここで「共通の基本スタブ」を再設定（各テストで上書きしてOK）（UUID/IDを “VALID_UUID” に統一）
    // 例：成功系／500系で使う固定ID
    when(converter.decodeUuidOrThrow(base64Id))
        .thenReturn(UUID.fromString(VALID_UUID));

    // encodeBase64 の共通スタブが必要なら、VALID_UUID から作った bytes に合わせる
    when(converter.encodeBase64(argThat(arr -> Arrays.equals(arr, studentId))))
        .thenReturn(base64Id);
  }

  // --------------------
  // helpers
  // --------------------
  /**
   * エラーレスポンスの互換検証ユーティリティ。
   * <p>
   * errorType/error、errorCode/code（int/文字列）等の差異を吸収して期待値を検証します。
   *
   * @param result MockMvcの実行結果
   * @param expectedHttp 期待するHTTPステータス
   * @param expectedType 期待するエラータイプ（例: {@code TYPE_MISMATCH}）
   * @param expectedCodeSubstring エラーコードに含まれるべき部分文字列（例: {@code "E404"}）
   * @param expectedMessageSubstringOrNull メッセージに含まれるべき部分文字列（不要ならnull）
   * @throws Exception レスポンス読み取りに失敗した場合
   */
  private void assertErrorCompat(MvcResult result, int expectedHttp,
      String expectedType, String expectedCodeSubstring,
      String expectedMessageSubstringOrNull) throws Exception {
    var resp = result.getResponse();
    assertThat(resp.getStatus()).isEqualTo(expectedHttp);

    var root = objectMapper.readTree(resp.getContentAsString());

    // errorType / error のどちらでもOK
    String actualType = root.hasNonNull("errorType")
        ? root.get("errorType").asText()
        : root.path("error").asText("");
    assertThat(actualType).isEqualTo(expectedType);

    // errorCode / code（int or String）どちらでもOK
    String actualCode = root.hasNonNull("errorCode")
        ? root.get("errorCode").asText()
        : (root.has("code")
            ? (root.get("code").isInt()
            ? String.valueOf(root.get("code").asInt())
            : root.get("code").asText())
            : "");
    assertThat(actualCode).contains(expectedCodeSubstring); // "E404" や "400" 部分一致

    if (expectedMessageSubstringOrNull != null) {
      String actualMsg = root.path("message").asText("");
      assertThat(actualMsg).contains(expectedMessageSubstringOrNull);
    }
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
    StudentRegistrationRequest request = new StudentRegistrationRequest();
    request.setStudent(studentDto);
    request.setCourses(List.of(courseDto1));

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

    StudentRegistrationRequest debug =
        objectMapper.readValue(body, StudentRegistrationRequest.class);

    System.out.println("DEBUG student=" + debug.getStudent());
    System.out.println("DEBUG courses=" + debug.getCourses());

    assertThat(debug.getStudent()).isNotNull();
    assertThat(debug.getCourses()).isNotNull();

    mockMvc.perform(post("/api/students")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(body))
        .andDo(print())
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
        .andExpect(jsonPath("$.courses[0].endDate").isEmpty())
        .andDo(result -> System.out.println(result.getResponse().getContentAsString()));

    // （任意の検証）
    verify(converter).toEntity(any(StudentDto.class));
    verify(converter).toEntityList(anyList(), any());
    verify(service).registerStudent(any(Student.class), anyList());
    verify(converter).toDetailDto(any(Student.class), anyList());
  }

  /**
   * バリデーションエラーがある状態で受講生を登録しようとしたときに、 適切なHTTPステータスとエラーレスポンスが返却されることを検証します。
   * <p>
   * 主な検証内容：
   * <ul>
   *   <li>ステータスコードが 400 Bad Request であること</li>
   *   <li>レスポンスボディに errorCode が "E001" として含まれること</li>
   *   <li>エラー詳細（errors）が配列として含まれていること</li>
   * </ul>
   *
   * @throws Exception HTTP通信の模擬処理中に例外が発生した場合
   */
  @Test
  public void registerStudent_バリデーションエラー発生時に適切なHTTPステータスとエラーレスポンスが返ること()
      throws Exception {
    String invalid = """
        {
          "student": {
            "fullName": "",
            "furigana": "",
            "nickname": "",
            "email": "invalid-email",
            "location": "",
            "age": -1,
            "gender": "",
            "remarks": ""
          },
          "courses": []
        }
        """;

    mockMvc.perform(post("/api/students")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalid))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("400"))
        .andExpect(jsonPath("$.errorCode").value("E001"))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThan(0)));
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
   * {@code includeDeleted=true} と {@code deletedOnly=true} を同時指定した場合に
   * 400エラー（不正リクエスト）が返ることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students}
   * <br>Params: {@code includeDeleted=true}, {@code deletedOnly=true}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>service.getStudentList(null, true, true) が {@code IllegalArgumentException} を送出</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvcでGET（両方true）を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>ステータス400で、メッセージに同時指定不可の旨が含まれる</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentList_論理削除と削除のみ指定が同時にtrueの場合_例外が返ること()
      throws Exception {
    when(service.getStudentList(null, true, true))
        .thenThrow(new IllegalArgumentException(
            "includeDeleted=true と deletedOnly=true は同時指定できません"));

    mockMvc.perform(get("/api/students")
            .param("includeDeleted", "true")
            .param("deletedOnly", "true"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value(
                "無効なリクエストです: includeDeleted=true と deletedOnly=true は同時指定できません"));
  }

  /**
   * {@code includeDeleted} に文字列など不正な型を指定した場合に
   * 型不一致エラー（E004/400）が返ることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students}
   * <br>Params: {@code includeDeleted=abc}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>When:
   * <ul>
   *   <li>MockMvcでGET（includeDeleted に不正な値）を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, errorType=TYPE_MISMATCH, errorCode=E004 を検証</li>
   *   <li>message に {@code includeDeleted} が含まれる</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentList_includeDeletedに文字列が指定された場合_型不一致エラーが返ること()
      throws Exception {
    mockMvc.perform(get("/api/students").param("includeDeleted", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.errorType").value("TYPE_MISMATCH"))   // ← ここを errorType に
        .andExpect(jsonPath("$.errorCode").value("E004"))
        // 部分一致（日本語の空白・引用符差で落ちにくい）
        .andExpect(jsonPath("$.message").value(containsString("includeDeleted")));
  }

  /**
   * {@code deletedOnly} に文字列など不正な型を指定した場合に
   * 型不一致エラー（E004/400）が返ることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students}
   * <br>Params: {@code deletedOnly=xyz}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>When:
   * <ul>
   *   <li>MockMvcでGET（deletedOnly に不正な値）を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, errorType=TYPE_MISMATCH, errorCode=E004 を検証</li>
   *   <li>message に {@code deletedOnly} が含まれる</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentList_deletedOnlyに文字列が指定された場合_型不一致エラーが返ること()
      throws Exception {
    mockMvc.perform(get("/api/students").param("deletedOnly", "xyz"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.errorType").value("TYPE_MISMATCH"))   // ← 同じく errorType
        .andExpect(jsonPath("$.errorCode").value("E004"))
        .andExpect(jsonPath("$.message").value(containsString("deletedOnly")));
  }

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
   * 存在しない受講生IDを指定した場合に 404（E404/NOT_FOUND）が返却されることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students/{studentId}}
   * <br>Status: {@code 404 NOT_FOUND}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(base64Id)} が 16バイトID を返す</li>
   *   <li>{@code service.findStudentById(studentId)} が {@code ResourceNotFoundException} を送出</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/{studentId}} を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=404, code=E404, error=NOT_FOUND を検証する</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentDetail_存在しないIDを指定した場合_404エラーが返ること() throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    when(service.findStudentById(studentId))
        .thenThrow(new ResourceNotFoundException("IDが見つかりません"));

    mockMvc.perform(get("/api/students/{studentId}", base64Id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  /**
   * Base64形式でないIDを指定した場合に、型不一致エラー（E004/TYPE_MISMATCH/400）が返却されることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(invalid)} が {@code IllegalArgumentException} を送出</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/{invalid}} を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E004, error=TYPE_MISMATCH を検証する</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentDetail_Base64形式でないIDを指定した場合_400エラーが返ること()
      throws Exception {

    String invalid = "@@invalid@@";
    when(converter.decodeBase64(invalid)).thenThrow(new IllegalArgumentException("Base64 error"));

    mockMvc.perform(get("/api/students/{studentId}", invalid))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E004"))
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"));
  }

  /**
   * Base64デコード後のバイト長がUUIDの16バイトと異なる場合に、E006/INVALID_REQUEST/400 が返却されることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(base64Id)} が長さ不正により {@code IllegalArgumentException} を送出</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/{studentId}} を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証する</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentDetail_デコード後のバイト長がUUIDと異なる場合_400エラーが返ること()
      throws Exception {

    when(converter.decodeBase64(base64Id))
        .thenThrow(new IllegalArgumentException("UUIDの形式が不正です"));

    mockMvc.perform(get("/api/students/{studentId}", base64Id))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
  }

  /**
   * studentId を指定せずにコレクションのルートを叩いた場合、404（NOT_FOUND）が返ることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students/}（パス変数未指定）
   * <br>Status: {@code 404 NOT_FOUND}
   * <p>When:
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/} を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=404, code=E404, error=NOT_FOUND を検証する</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentDetail_studentIdが空文字の場合_404エラーが返ること() throws Exception {

    mockMvc.perform(get("/api/students/"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
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
   * バリデーションエラーが発生した更新要求に対して 400（E001/VALIDATION_FAILED）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>When:
   * <ul>
   *   <li>必須項目が欠落／不正のJSONを送信</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, errorCode=E001, errors配列あり</li>
   *   <li>converter と service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_バリデーションエラーが発生する場合_400を返すこと() throws Exception {

    String invalid = """
        {
          "student": {
            "fullName": "",
            "furigana": "",
            "nickname": "",
            "email": "invalid-email",
            "location": "",
            "age": -1,
            "gender": "",
            "remarks": ""
          },
          "courses": []
        }
        """;

    mockMvc.perform(put("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalid))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("400"))
        .andExpect(jsonPath("$.errorCode").value("E001"))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors.length()").value(Matchers.greaterThan(0)))
        .andExpect(jsonPath("$.code").value(400));

    // バリデーションで落ちる想定なので依存には触らない
    verifyNoInteractions(converter, service);
  }

  /**
   * 存在しない受講生IDで更新した場合に 404（E404/NOT_FOUND）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 404 NOT_FOUND}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeUuidOrThrow(base64Id)} は有効IDを返す</li>
   *   <li>{@code service.updateStudentWithCourses(...)} が {@code ResourceNotFoundException} を送出</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvcでPUTを実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=404, code=E404, error=NOT_FOUND を検証</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_存在しない受講生IDを指定した場合_404を返すこと() throws Exception {
    var valid = json(new StudentRegistrationRequest() {{
      setStudent(studentDto);
      setCourses(List.of());
    }});

    when(converter.decodeUuidOrThrow(base64Id)).thenReturn(UUID.fromString(VALID_UUID));
    when(converter.toEntity(studentDto)).thenReturn(student);
    when(converter.toEntityList(eq(List.of()),
        argThat(arr -> Arrays.equals(arr, studentId)))).thenReturn(List.of());

    // 更新後の再取得で見つからないケースを想定
    doNothing().when(service).updateStudent(any(Student.class), anyList());
    doThrow(new ResourceNotFoundException("IDが見つかりません"))
        .when(service).updateStudentWithCourses(any(Student.class), anyList());

    mockMvc.perform(put("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(valid))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND"))
        .andExpect(jsonPath("$.code").value(404));
  }

  /**
   * Base64形式が不正なIDで更新要求した場合に 400（E006/INVALID_REQUEST）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{invalidId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeUuidOrThrow(invalidId)} が {@code InvalidIdFormatException} を送出</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvcでPUTを実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証</li>
   *   <li>service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_base64形式が不正な場合_400を返すこと() throws Exception {
    String invalidId = "invalid_base64";

    // Base64として不正 → 変換時点で独自例外（E006）を投げる前提
    doThrow(new InvalidIdFormatException("IDの形式が不正です（Base64）"))
        .when(converter).decodeUuidOrThrow(invalidId);

    String body = json(new StudentRegistrationRequest() {{
      setStudent(studentDto);
      setCourses(List.of(courseDto));
    }});

    mockMvc.perform(put("/api/students/{studentId}", invalidId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.code").value(400));

    // 以降は到達しない
    verifyNoInteractions(service);
  }

  /**
   * UUID長（16バイト）でないID（Base64は正しい）で更新要求した場合に 400（E006）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeUuidOrThrow(idWithWrongLength)} が {@code InvalidIdFormatException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証</li>
   *   <li>service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_UUID形式が不正な場合_400を返すこと() throws Exception {
    // Base64 としては正しいが UUID(16バイト)ではない 10バイトのIDを用意
    String idWithWrongLength = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[10]);

    // 長さ不正も decode 時点で InvalidIdFormatException を投げる前提（E006）
    doThrow(new InvalidIdFormatException("IDの形式が不正です（UUID）"))
        .when(converter).decodeUuidOrThrow(idWithWrongLength);

    // リクエストボディ（バリデーションは通る想定）
    String body = json(new StudentRegistrationRequest() {{
      setStudent(studentDto);
      setCourses(List.of(courseDto));
    }});

    // 実行：controller 側で「長さ != 16」を検知して 400 を返すはず
    mockMvc.perform(put("/api/students/{studentId}", idWithWrongLength)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.code").value(400));

    verifyNoInteractions(service);
  }

  /**
   * 更新要求のボディが空の場合に 400（E003/MISSING_PARAMETER）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>When:
   * <ul>
   *   <li>空ボディでPUTを実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>errorCode=E003, errorType=MISSING_PARAMETER, code=400 を検証</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_リクエストボディが空の場合_400を返すこと() throws Exception {

    mockMvc.perform(put("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("E003"))
        .andExpect(jsonPath("$.errorType").value("MISSING_PARAMETER"))
        .andExpect(jsonPath("$.code").value(400));

    // @Valid 前にdecodeしない設計なら、ここも converter に触らない想定にできます。
    // 設計に合わせて下行はコメントアウト可
    // verifyNoInteractions(converter, service);
  }

  /**
   * 更新要求で {@code student} が {@code null} の場合に 400（E001/VALIDATION_FAILED）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>When:
   * <ul>
   *   <li>{@code {"student":null,"courses":[]}} を送信</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>details[0].field=student を含むバリデーションエラーを検証</li>
   *   <li>converter と service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_student情報がnullの場合_400を返すこと() throws Exception {

    // バリデーションで弾く想定なので decode すら呼ばれない（@Valid → その後 decode）
    String body = """
        {
          "student": null,
          "courses": []
        }
        """;

    mockMvc.perform(put("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("E001"))
        .andExpect(jsonPath("$.errorType").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.details[0].field").value("student"))
        .andExpect(jsonPath("$.code").value(400));

    verifyNoInteractions(converter, service);
  }

  /**
   * 更新要求で {@code courses} が {@code null} の場合に 400（E001/VALIDATION_FAILED）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>When:
   * <ul>
   *   <li>courses=null を含むJSONでPUTを実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>details[*].field に {@code courses} を含む</li>
   *   <li>converter と service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_coursesがnullの場合_400を返すこと() throws Exception {
    // Arrange
    var req = new StudentRegistrationRequest();
    req.setStudent(studentDto);   // ← @BeforeEach で作ったものをそのまま使用
    req.setCourses(null);         // ← 検証ポイント
    // 必須なら: req.setAppendCourses(false);

    // Act & Assert
    mockMvc.perform(put("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("E001"))
        .andExpect(jsonPath("$.errorType").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.details[*].field", org.hamcrest.Matchers.hasItem("courses")))
        .andExpect(jsonPath("$.code").value(400));

    // バリデーションで弾かれるので下位は呼ばれない想定
    verifyNoInteractions(converter, service);
  }

  /**
   * 更新処理中の想定外例外を 500（E999/INTERNAL_SERVER_ERROR）としてハンドリングすることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 500 INTERNAL_SERVER_ERROR}
   * <p>Given:
   * <ul>
   *   <li>前段の変換は成功する</li>
   *   <li>{@code service.updateStudentWithCourses(...)} が実行時例外を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=500, code=E999, error=INTERNAL_SERVER_ERROR を検証</li>
   *   <li>{@code encodeBase64} / 3引数版 {@code toDetailDto} は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_想定外の例外が発生した場合_500を返すこと() throws Exception {
    // Arrange: 正常リクエストを用意（バリデーションは通す）
    var req = new StudentRegistrationRequest();
    req.setStudent(studentDto);              // @BeforeEach のものを利用
    req.setCourses(List.of(courseDto));      // null ではなく、1件以上を入れて通過

    // 変換系のモック（ここまでは正常に進む）
    when(converter.decodeUuidOrThrow(base64Id)).thenReturn(UUID.fromString(VALID_UUID));
    when(converter.toEntity(studentDto)).thenReturn(student);
    when(converter.toEntityList(
        eq(List.of(courseDto)),
        argThat(arr -> Arrays.equals(arr, studentId))
    )).thenReturn(courses);

    // ★ ここで expected を用意（JSONの期待内容に合わせる）
    StudentDetailDto expected = new StudentDetailDto(studentDto, List.of(courseDto));

    // ★ 3引数版 toDetailDto の stub
    when(converter.toDetailDto(
        any(Student.class),             // service が別インスタンスを返す可能性に備えて any()
        same(courses),                  // toEntityList の戻りをそのまま使うなら same() が安全
        eq(base64Id)                    // encodeBase64 の戻り
    )).thenReturn(expected);
    // Service の更新時に想定外例外を発生させる
    doThrow(new RuntimeException("Unexpected failure"))
        .when(service).updateStudentWithCourses(any(Student.class), anyList());

    // Act & Assert
    mockMvc.perform(put("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(req)))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.code").value("E999"))
        .andExpect(jsonPath("$.code").value(500));

    // 呼び出し検証（更新で落ちたので DTO 化までは行かない）
    verify(converter).decodeUuidOrThrow(base64Id);
    verify(converter).toEntity(studentDto);
    verify(converter).toEntityList(anyList(),
        argThat(arr -> Arrays.equals(arr, studentId)));

    verify(service).updateStudentWithCourses(any(Student.class), anyList());

// ★到達しないことを明示
    verify(converter, never()).encodeBase64(any(byte[].class));
    verify(converter, never()).toDetailDto(any(), anyList(), anyString());

// 余計な呼び出しが無いこと
    verifyNoMoreInteractions(converter, service);
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

    StudentRegistrationRequest request = new StudentRegistrationRequest();
    request.setStudent(new StudentDto());
    request.setCourses(List.of(courseDto));
    request.setAppendCourses(false);

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
            .content(json(request)))
        .andExpect(status().isOk());
  }

  /**
   * 部分更新で該当受講生が存在しない場合に 404（E404/NOT_FOUND）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 404 NOT_FOUND}
   * <p>Given:
   * <ul>
   *   <li>{@code service.findStudentById(studentId)} が {@code ResourceNotFoundException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=404, code=E404, error=NOT_FOUND, メッセージ本文を検証</li>
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

    mockMvc.perform(patch("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("該当する受講生が見つかりません"));
  }

  /**
   * 部分更新でBase64形式が不正なIDの場合に 400（E006/INVALID_REQUEST）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(base64Id)} が {@code IllegalArgumentException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証</li>
   *   <li>service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void partialUpdateStudent_base64形式が不正な場合_400を返すこと() throws Exception {

    StudentRegistrationRequest request = new StudentRegistrationRequest();
    request.setStudent(studentDto);
    request.setCourses(List.of());
    request.setAppendCourses(false);

    when(converter.decodeBase64(base64Id))
        .thenThrow(new IllegalArgumentException("UUIDの形式が不正です"));

    mockMvc.perform(patch("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

    verify(converter).decodeBase64(base64Id);
    verifyNoMoreInteractions(converter);
    verifyNoInteractions(service);
  }

  /**
   * 部分更新でUUID長が不正なIDの場合に 400（E006/INVALID_REQUEST）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(invalidId)} が {@code IllegalArgumentException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証</li>
   *   <li>service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void partialUpdateStudent_UUID形式が不正な場合_400を返すこと() throws Exception {

    String invalidId = "AAAAAAAAAAAAAAAAAAAAAA=="; // 長さ不正相当
    StudentRegistrationRequest request = new StudentRegistrationRequest();
    request.setStudent(studentDto);
    request.setCourses(List.of());
    request.setAppendCourses(false);

    when(converter.decodeBase64(invalidId))
        .thenThrow(new IllegalArgumentException("UUIDの形式が不正です"));

    mockMvc.perform(patch("/api/students/{studentId}", invalidId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

    verify(converter).decodeBase64(invalidId);
    verifyNoMoreInteractions(converter);
    verifyNoInteractions(service);
  }

  /**
   * 部分更新のボディが空の場合に 400（E003/MISSING_PARAMETER）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * Then:
   * <ul>
   *   <li>errorCode=E003, errorType=MISSING_PARAMETER を検証</li>
   *   <li>converter と service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void partialUpdateStudent_リクエストボディが空の場合_400を返すこと() throws Exception {

    mockMvc.perform(patch("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("E003"))
        .andExpect(jsonPath("$.errorType").value("MISSING_PARAMETER"));

    verifyNoInteractions(converter, service);
  }

  /**
   * 部分更新で空オブジェクトを送った場合に 400（E003/MISSING_PARAMETER）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * Then:
   * <ul>
   *   <li>errorCode=E003, errorType=MISSING_PARAMETER を検証</li>
   *   <li>converter と service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */

  @Test
  public void partialUpdateStudent_空JSONオブジェクトの場合_400を返すこと() throws Exception {

    mockMvc.perform(patch("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("E003"))
        .andExpect(jsonPath("$.errorType").value("MISSING_PARAMETER"));
    verifyNoInteractions(converter, service);
  }

  /**
   * 部分更新で {@code student} が {@code null} の場合に 400（E001/VALIDATION_FAILED）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * Then:
   * <ul>
   *   <li>details[0].field=student を検証</li>
   *   <li>converter と service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void partialUpdateStudent_student情報がnullの場合_400を返すこと() throws Exception {

    mockMvc.perform(patch("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"student\":null,\"courses\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("E001"))
        .andExpect(jsonPath("$.errorType").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.details[0].field").value("student"));

    verifyNoInteractions(converter, service);
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

    // 置換モードなので partialUpdateStudent は呼ばれる（空でもOK、Service側が無視）
    verify(service).updateStudentInfoOnly(existing);

    // appendCourses=false 相当なので「既存コースの再取得」は呼ばれない
    verify(service).searchCoursesByStudentId(studentId);
  }

  /**
   * （置換モード）部分更新の正常系として service.partialUpdateStudent が期待どおり呼ばれることを検証します。
   * <p>
   * ※テスト名と異なり、このテストでは 200 OK を検証しています。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 200 OK}
   * <p>Given:
   * <ul>
   *   <li>student と courses を含む有効なJSON</li>
   *   <li>{@code converter/toEntity...} が正常に動作</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>{@code service.partialUpdateStudent(existing, newCourses)} が同値リストで呼ばれる</li>
   *   <li>{@code service.searchCoursesByStudentId} は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
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

    mockMvc.perform(patch("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk());

    // appendCourses 未指定 ⇒ デフォルト(false) ⇒ 置換モード
    verify(service).partialUpdateStudent(eq(existing), argThat(list ->
        list != null && list.size() == newCourses.size()
            && list.containsAll(newCourses) && newCourses.containsAll(list)));

    verify(service, never()).searchCoursesByStudentId(any());
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
   * 論理削除対象が存在しない場合に 404（E404/NOT_FOUND）が返ることを検証します。
   * <p>
   * Endpoint: {@code DELETE /api/students/{studentId}}
   * <br>Status: {@code 404 NOT_FOUND}
   * <p>Given:
   * <ul>
   *   <li>{@code service.softDeleteStudent(studentId)} が {@code ResourceNotFoundException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=404, code=E404, error=NOT_FOUND を検証</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void deleteStudent_対象受講生が存在しない場合_404を返すこと() throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    doThrow(new ResourceNotFoundException("IDが見つかりません"))
        .when(service).softDeleteStudent(eq(studentId));

    mockMvc.perform(delete("/api/students/{studentId}", base64Id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));

    verify(converter).decodeBase64(base64Id);
    verify(service).softDeleteStudent(eq(studentId));
    verifyNoMoreInteractions(service);
  }

  /**
   * 論理削除でBase64形式が不正なIDを指定した場合に 400（E006/INVALID_REQUEST）が返ることを検証します。
   * <p>
   * Endpoint: {@code DELETE /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(invalid)} が {@code IllegalArgumentException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証</li>
   *   <li>service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void deleteStudent_studentIdのBase64形式が不正な場合_400を返すこと() throws Exception {

    String invalid = "invalid_base64";
    when(converter.decodeBase64(invalid))
        .thenThrow(new IllegalArgumentException("UUIDの形式が不正です"));

    mockMvc.perform(delete("/api/students/{studentId}", invalid))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

    verify(converter).decodeBase64(invalid);
    verifyNoInteractions(service);
  }

  /**
   * 論理削除処理中の想定外例外を 500（E999/INTERNAL_SERVER_ERROR）としてハンドリングすることを検証します。
   * <p>
   * Endpoint: {@code DELETE /api/students/{studentId}}
   * <br>Status: {@code 500 INTERNAL_SERVER_ERROR}
   * <p>Given:
   * <ul>
   *   <li>{@code service.softDeleteStudent(studentId)} が実行時例外を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=500, code=E999, error=INTERNAL_SERVER_ERROR を検証</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void deleteStudent_想定外の例外が発生した場合_500を返すこと() throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    doThrow(new RuntimeException("Unexpected failure"))
        .when(service).softDeleteStudent(eq(studentId));

    mockMvc.perform(delete("/api/students/{studentId}", base64Id))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.code").value("E999"));

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

  /**
   * 復元対象が存在しない／未削除の場合に 404（E404/RESOURCE_NOT_FOUND）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}/restore}
   * <br>Status: {@code 404 NOT_FOUND}
   * <p>Given:
   * <ul>
   *   <li>{@code service.restoreStudent(studentId)} が {@code ResourceNotFoundException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=404, code=E404, error=RESOURCE_NOT_FOUND を検証</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void restoreStudent_対象受講生が存在しないまたは未削除の場合_404を返すこと()
      throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    doThrow(new ResourceNotFoundException("IDが見つかりません"))
        .when(service).restoreStudent(eq(studentId));

    mockMvc.perform(patch("/api/students/{studentId}/restore", base64Id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("E404"))
        .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

    verify(converter).decodeBase64(base64Id);
    verify(service).restoreStudent(eq(studentId));
    verifyNoMoreInteractions(service);
  }

  /**
   * 復元でBase64形式が不正なIDを指定した場合に 400（E006/INVALID_REQUEST）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}/restore}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(invalid)} が {@code IllegalArgumentException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証</li>
   *   <li>service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void restoreStudent_Base64形式が不正の場合_400を返すこと() throws Exception {

    String invalid = "invalid_base64";
    when(converter.decodeBase64(invalid))
        .thenThrow(new IllegalArgumentException("UUIDの形式が不正です"));

    mockMvc.perform(patch("/api/students/{studentId}/restore", invalid))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

    verify(converter).decodeBase64(invalid);
    verifyNoInteractions(service);
  }

  /**
   * 復元処理中の想定外例外を 500（E999/INTERNAL_SERVER_ERROR）としてハンドリングすることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}/restore}
   * <br>Status: {@code 500 INTERNAL_SERVER_ERROR}
   * <p>Given:
   * <ul>
   *   <li>{@code service.restoreStudent(studentId)} が実行時例外を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=500, code=E999, error=INTERNAL_SERVER_ERROR を検証</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void restoreStudent_想定外の例外が発生した場合_500を返すこと() throws Exception {

    when(converter.decodeBase64(base64Id)).thenReturn(studentId);
    doThrow(new RuntimeException("Unexpected failure"))
        .when(service).restoreStudent(eq(studentId));

    mockMvc.perform(patch("/api/students/{studentId}/restore", base64Id))
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
   * <p>
   * Endpoint: {@code GET /api/students/test-missing-param}
   * <br>Status: {@code 400 BAD_REQUEST}
   * When:
   * <ul>
   *   <li>必須の {@code keyword} を付けずに呼び出す</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>errorCode=E003 を検証</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void testMissingServletRequestParameterException() throws Exception {
    mockMvc.perform(get("/api/students/test-missing-param")) // keyword を指定しない
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("E003")); // GlobalExceptionHandler 側のエラーコードに応じて変更
  }

  /**
   * MethodArgumentTypeMismatchException のテスト。 'id' に不正な型（例: abc）を指定して 400 を期待。
   * 型不一致時（MethodArgumentTypeMismatchException）に 400/E004 が返ることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students/test-type}
   * <br>Status: {@code 400 BAD_REQUEST}
   * When:
   * <ul>
   *   <li>{@code id} に数値以外（例: {@code "abc"}}）を指定</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>errorCode=E004 を検証</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void testMethodArgumentTypeMismatchException() throws Exception {
    mockMvc.perform(get("/api/students/test-type")
            .param("id", "abc")) // int 型に変換できない文字列
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("E004"));
  }
}

















