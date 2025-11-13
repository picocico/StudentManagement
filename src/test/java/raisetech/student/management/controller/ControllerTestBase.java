package raisetech.student.management.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.ResultActions;
import raisetech.student.management.config.TestMockConfig;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentCourseDto;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentDto;
import raisetech.student.management.exception.GlobalExceptionHandler;
import raisetech.student.management.service.StudentService;
import raisetech.student.management.util.IdCodec;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = {StudentController.class, DebugStudentController.class})
@Import({GlobalExceptionHandler.class, TestMockConfig.class})
@ImportAutoConfiguration(exclude = {GsonAutoConfiguration.class})
@ActiveProfiles("test")
abstract class ControllerTestBase {

  /**
   * Controller のテストにおいて、HTTPリクエストおよびレスポンスを模倣するためのテスト用オブジェクト。
   *
   * <p>{@code @WebMvcTest} などでSpring MVCの振る舞いを検証する際に使用される。
   */
  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  /**
   * モック化された {@link StudentService}。
   *
   * <p>コントローラの依存性として注入され、サービス層のロジックをテスト対象から切り離します
   */
  @Autowired
  protected StudentService service;

  /**
   * テスト対象の {@link StudentController} に注入されるモックの {@link StudentConverter}。
   *
   * <p>実際のエンティティ・DTO変換処理は行わず、必要に応じてスタブ化された動作により、 コントローラーの単体テストを実現します。
   */
  @Autowired
  protected StudentConverter converter;

  @Autowired
  protected IdCodec idCodec;

  protected static final boolean DEBUG =
      Boolean.parseBoolean(System.getProperty("debugTests", "false"))
          || "true".equalsIgnoreCase(System.getenv("DEBUG_TESTS"));

  // .andDo(...) に差せる条件付きハンドラ
  // デフォルトは無出力（-DDEBUG_MOCKMVC=true でのみ有効化）
  protected static org.springframework.test.web.servlet.ResultHandler maybePrint() {
    // デフォルトは無出力
    if (!Boolean.parseBoolean(System.getProperty("DEBUG_MOCKMVC", "false"))) {
      return result -> {
      }; // no-op
    }
    // デバッグ時のみ本文表示やprint()を使う
    return org.springframework.test.web.servlet.result.MockMvcResultHandlers.print();
  }

  // テスト共通で使う「常に16バイト」のID
  protected static final String VALID_UUID = "123e4567-e89b-12d3-a456-426655440000";

  protected byte[] studentId;
  protected String base64Id;

  protected String fullName;
  protected String furigana;
  protected String nickname;
  protected String email;
  protected String location;
  protected int age;
  protected String gender;
  protected String remarks;

  protected String courseName;
  protected String secondCourseName;

  protected Student student;
  protected StudentDto studentDto;

  protected StudentCourse course1;
  protected StudentCourse course2;
  protected List<StudentCourse> courses;

  protected StudentCourseDto courseDto;
  protected StudentCourseDto courseDto1;
  protected StudentCourseDto courseDto2;

  protected StudentDetailDto detailDto;
  protected StudentDetailDto detailDto1;
  protected StudentDetailDto detailDto2;

  // 便利関数

  /**
   * オブジェクトをJSON文字列にシリアライズします。
   *
   * @param o シリアライズ対象オブジェクト
   * @return JSON文字列
   * @throws Exception シリアライズに失敗した場合
   */
  protected String json(Object o) throws Exception {
    return objectMapper.writeValueAsString(o);
  }

  protected ResultActions postJson(String path, Object body) throws Exception {
    return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json(body)));
  }

  /**
   * 各テスト実行前に{@code MockMvc}を共通初期化を行います。
   *
   * <p>Given:
   *
   * <ul>
   *   <li>固定のUUID/16バイトID/base64Idの生成
   *   <li>受講生・コースのダミーデータおよびDTOの用意
   *   <li>モックの完全リセットと、共通スタブ（decodeUuidOrThrow, encodeBase64）の再設定
   * </ul>
   * <p>
   * Then:
   *
   * <ul>
   *   <li>以降の各テストが同一前提で安定して実行できる状態になる
   * </ul>
   */
  @BeforeEach
  void setupAll() {
    Mockito.reset(service, converter, idCodec);

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
    when(idCodec.decodeUuidOrThrow(base64Id)).thenReturn(UUID.fromString(VALID_UUID));

    when(idCodec.decodeUuidBytesOrThrow(eq(base64Id))).thenReturn(studentId); // ダミーのBINARY(16)
    when(idCodec.decodeUuidOrThrow(eq(base64Id))).thenReturn(UUID.fromString(VALID_UUID));

    // encodeBase64 の共通スタブが必要なら、VALID_UUID から作った bytes に合わせる
    when(converter.encodeBase64(argThat(arr -> Arrays.equals(arr, studentId))))
        .thenReturn(base64Id);

    // 共通ハッピーパス・スタブ（必要なら各テストで上書きOK）
    stubConverterHappyPath();
    stubServiceHappyPath();
  }

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
  protected static String base64FromUuid(String uuid) {
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
  protected static byte[] bytesFromUuid(String uuid) {
    var u = UUID.fromString(uuid);
    var bb = ByteBuffer.allocate(16);
    bb.putLong(u.getMostSignificantBits());
    bb.putLong(u.getLeastSignificantBits());
    return bb.array();
  }

  // --------------------
  // helpers
  // --------------------

  /**
   * エラーレスポンスの互換検証ユーティリティ。
   *
   * <p>errorType/error、errorCode/code（int/文字列）等の差異を吸収して期待値を検証します。
   *
   * @param result                         MockMvcの実行結果
   * @param expectedHttp                   期待するHTTPステータス
   * @param expectedType                   期待するエラータイプ（例: {@code TYPE_MISMATCH}）
   * @param expectedCodeSubstring          エラーコードに含まれるべき部分文字列（例: {@code "E404"}）
   * @param expectedMessageSubstringOrNull メッセージに含まれるべき部分文字列（不要ならnull）
   * @throws Exception レスポンス読み取りに失敗した場合
   */
  protected void assertErrorCompat(
      MvcResult result,
      int expectedHttp,
      String expectedType,
      String expectedCodeSubstring,
      String expectedMessageSubstringOrNull)
      throws Exception {
    var resp = result.getResponse();
    assertThat(resp.getStatus()).isEqualTo(expectedHttp);

    var root = objectMapper.readTree(resp.getContentAsString());

    // error は必須（旧: errorType は非対応）
    String actualType = root.path("error").asText("");
    assertThat(actualType).as("error フィールドは必須").isEqualTo(expectedType);

    // code は必須（常に "E***" の String を想定）
    String actualCode = root.path("code").asText("");
    assertThat(actualCode)
        .as("code フィールドは必須（例: E001, E004）")
        .isNotBlank()
        .contains(expectedCodeSubstring);

    // 旧キーは存在しないことを明示チェック（Smoke の契約を固定化）
    assertThat(root.has("errorCode")).as("旧キー errorCode は出力しない").isFalse();
    assertThat(root.has("errorType")).as("旧キー errorType は出力しない").isFalse();

    if (expectedMessageSubstringOrNull != null) {
      String actualMsg = root.path("message").asText("");
      assertThat(actualMsg).contains(expectedMessageSubstringOrNull);
    }
  }

  // --------------------
  // stubbing helpers
  // --------------------

  /**
   * Converter の「共通ハッピーパス」スタブを設定します。
   *
   * <p>Controller の単体テストで、変換レイヤーの振る舞いを安定化させるための 最低限のダミー挙動（成功シナリオ）を一括で登録します。
   * 個々のテストで上書き（再スタブ）して構いません。
   *
   * <h4>設定内容</h4>
   *
   * <ul>
   *   <li>{@code decodeBase64(String)}: 任意入力 → ダミー16進配列（{@code byte[]{1,2,3}}）
   *   <li>{@code toEntity(...)}: 任意入力（null 可）→ 空の {@code Student} を返却
   *   <li>{@code mergeStudent(...)}: void メソッド → 何もしない（素通し）
   *   <li>{@code toEntityList(...)}: 任意入力 → 空リスト
   *   <li>{@code toDetailDto(...)}: 最小限の {@code StudentDetailDto}（または mock）を返却
   * </ul>
   *
   * <h4>意図</h4>
   * <p>
   * 変換の正当性そのものはこのテストでは検証対象外とし、 Controller の分岐やエラーハンドリングだけに焦点を当てます。
   *
   * <h4>注意</h4>
   * <p>
   * {@code StudentDetailDto} にデフォルトコンストラクタが無い場合は mock 返却にフォールバックします。
   */
  // 共通（ハッピーパス）スタブを流し込む
  protected void stubConverterHappyPath() {
    // Controller で decodeBase64 を呼ぶケースに対応
    when(converter.decodeBase64(anyString())).thenReturn(new byte[]{1, 2, 3});

    // toEntity(null 可)。必要最低限の空オブジェクトでOK
    when(converter.toEntity(any())).thenAnswer(inv -> new Student());

    // mergeStudent は void：素通し
    doAnswer(inv -> null).when(converter).mergeStudent(any(Student.class), any(Student.class));

    // toEntityList：空リストでOK（個別テストで上書き可能）
    when(converter.toEntityList(anyList(), any())).thenReturn(List.of());

    // toDetailDto：最小のダミーを返す
    // new StudentDetailDto() が無ければ、下の行をコメントアウトして、代わりに mock を返してOK
    try {
      when(converter.toDetailDto(any(Student.class), anyList()))
          .thenAnswer(
              inv -> {
                try {
                  return new StudentDetailDto(); // ← デフォコン無ければ次行に切替
                } catch (Throwable t) {
                  return org.mockito.Mockito.mock(StudentDetailDto.class);
                }
              });
    } catch (Throwable ignore) {
      when(converter.toDetailDto(any(Student.class), anyList()))
          .thenReturn(org.mockito.Mockito.mock(StudentDetailDto.class));
    }
  }

  /**
   * Service の「共通ハッピーパス」スタブを設定します。
   *
   * <p>DB アクセスを伴うサービス層を無害化し、Controller の挙動を疎結合に 検証するための標準スタブです。個々のテストで上書き可能です。
   *
   * <h4>設定内容</h4>
   *
   * <ul>
   *   <li>{@code findStudentById(...)}: 新規 {@code Student} を返却
   *   <li>{@code searchCoursesByStudentId(...)}: 空リストを返却
   *   <li>{@code updateStudentInfoOnly / appendCourses / replaceCourses}: void → 何もしない
   * </ul>
   *
   * <h4>意図</h4>
   * <p>
   * 永続化やビジネスロジックの副作用を排し、Controller レベルの 入出力・例外ハンドリングに集中してテストできるようにします。
   */
  protected void stubServiceHappyPath() {
    when(service.findStudentById(any())).thenReturn(new Student());
    when(service.searchCoursesByStudentId(any())).thenReturn(List.of());

    // void メソッドは doNothing
    doNothing().when(service).updateStudentInfoOnly(any(Student.class));
    doNothing().when(service).appendCourses(any(), anyList());
    doNothing().when(service).replaceCourses(any(), anyList());
  }

  /**
   * 「想定外の実行時例外（500/E999 相当）」を能動的に発火させるための補助。
   *
   * <p>早い段階（例：{@code decodeBase64(...)}）で {@link RuntimeException} を投げるようにスタブし、
   * 後続のサービス呼び出しへ到達しない経路を作ります。
   *
   * <h4>用途</h4>
   *
   * <ul>
   *   <li>GlobalExceptionHandler の 500 系ハンドリング（メッセージ／コード／ログ）の検証
   *   <li>{@code NeverWantedButInvoked} 回避（サービスが呼ばれないことの保証）
   * </ul>
   *
   * <h4>使い方</h4>
   * <p>
   * 対象テストメソッドの冒頭で本メソッドを呼び出してから、通常通り MockMvc を実行します。
   */
  protected void makeConverterThrowEarly() {
    when(converter.decodeBase64(anyString())).thenThrow(new RuntimeException("boom"));
  }
}
