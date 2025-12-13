package raisetech.student.management.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentCourseDto;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentDto;
import raisetech.student.management.exception.InvalidIdFormatException;

/**
 * {@link StudentConverter} の単体テストクラス。
 *
 * <p>主な検証対象は次の通りです。
 * <ul>
 *   <li>ID 変換（UUID 由来の byte[16] と UUID 文字列の相互変換）</li>
 *   <li>Student / StudentCourse と各種 DTO 間の項目移送</li>
 *   <li>集約変換（Student ＋ StudentCourse → StudentDetailDto）</li>
 *   <li>部分更新マージ処理（{@link StudentConverter#mergeStudent(Student, Student)}）</li>
 * </ul>
 *
 */
@ExtendWith(MockitoExtension.class)
class StudentConverterTest {

  /**
   * テスト対象となるコンバータ。
   */
  @InjectMocks
  private StudentConverter converter;

  // テスト用の固定UUID文字列
  private static final String UUID_STRING = "123e4567-e89b-12d3-a456-426614174000";
  private static final String UUID_STRING_B = "123e4567-e89b-12d3-a456-426614174001";
  private UUID uuid;


  @BeforeEach
  void setUp() {
    uuid = UUID.fromString(UUID_STRING);
  }

  // ------------------------------------------------------------
  // ID変換メソッドのテスト
  // ------------------------------------------------------------

  /**
   * ID 変換系メソッド（UUID ⇔ byte[]、文字列 ID デコード）のテストグループ。
   */
  @Nested
  class IdConversionTest {

    /**
     * {@link StudentConverter#encodeUuidString(UUID)} が UUID を標準的な文字列表現に変換できることを検証します。
     *
     * <p>内部の実装詳細（どのユーティリティを使うか）には依存せず、
     * 「与えた UUID から期待通りの文字列が返るか」にフォーカスしたテストです。
     */
    @Test
    void encodeUuidString_正常系_UUIDを文字列に変換できること() {

      String actual = converter.encodeUuidString(uuid);
      // 期待値と結果が完全に一致することを確認
      // FIXED_UUID_BYTESのUUIDエンコード値
      assertThat(actual).isEqualTo(UUID_STRING);
    }

    @Test
    void decodeUuidStringOrThrow_正常系_UUID文字列をUUIDに変換できること() {
      UUID actual = converter.decodeUuidStringOrThrow(UUID_STRING);

      assertThat(actual).isEqualTo(UUID.fromString(UUID_STRING));
    }

    /**
     * {@link StudentConverter# decodeUuidOrThrow(String)} にnullや空文字が渡された場合、
     * {@link InvalidIdFormatException}（「" "」）がスローされることを検証します。
     */
    @Test
    void decodeUuidStringOrThrow_異常系_nullや空文字ならInvalidIdFormatException() {
      assertThatThrownBy(() -> converter.decodeUuidStringOrThrow(null))
          .isInstanceOf(InvalidIdFormatException.class);

      assertThatThrownBy(() -> converter.decodeUuidStringOrThrow("  "))
          .isInstanceOf(InvalidIdFormatException.class);
    }

    @Test
    void decodeUuidStringOrThrow_異常系_不正なUUID文字列ならInvalidIdFormatException() {
      assertThatThrownBy(() -> converter.decodeUuidStringOrThrow("not-a-uuid"))
          .isInstanceOf(InvalidIdFormatException.class)
          .hasMessageContaining("IDの形式が不正です");
    }
  }

// ------------------------------------------------------------
//　DTO ⇔ エンティティ 変換メソッドのテスト
// ------------------------------------------------------------

  /**
   * DTO とエンティティ間の変換ロジックを検証するテストグループ。
   *
   * <p>主に以下を対象とします。
   * <ul>
   *   <li>{@link StudentDto} ⇔ {@link Student}</li>
   *   <li>{@link StudentCourseDto} ⇔ {@link StudentCourse}</li>
   *   <li>リスト変換・新規 ID 採番の挙動</li>
   *   <li>集約 DTO／部分更新マージ処理</li>
   * </ul>
   */
  @Nested
  class DtoEntityConversionTest {

    /**
     * {@link StudentConverter#toEntity(StudentDto)} が、 ID 付きの {@link StudentDto} を正しく
     * {@link Student} へ変換していることを検証します。
     */
    @Test
    void toEntity_StudentDto_IDあり_全フィールドが正しくマッピングされIDがデコードされること() {
      StudentDto inputDto = new StudentDto(
          UUID_STRING,
          "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
          "Tokyo", 25, "Male", "備考", false
      );

      // 変換実行
      Student result = converter.toEntity(inputDto);

      // 検証
      // 1. IDが正しくデコードされているか
      assertThat(result.getStudentId()).isEqualTo(UUID.fromString(UUID_STRING));
      // 2. 他のフィールドが正しくマッピングされているか
      assertThat(result.getFullName()).isEqualTo("山田 太郎");
      assertThat(result.getFurigana()).isEqualTo("ヤマダ タロウ");
      assertThat(result.getNickname()).isEqualTo("Taro");
      assertThat(result.getEmail()).isEqualTo("taro@example.com");
      assertThat(result.getLocation()).isEqualTo("Tokyo");
      assertThat(result.getAge()).isEqualTo(25);
      assertThat(result.getGender()).isEqualTo("Male");
      assertThat(result.getRemarks()).isEqualTo("備考");
      assertThat(result.getDeleted()).isFalse();
    }

    /**
     * {@link StudentConverter#toEntity(StudentDto)} において、 ID が未指定の場合（null）のときに 新規 ID
     * が採番されることを検証します。
     */
    @Test
    void toEntity_StudentDto_IDなし_新規にランダムIDが生成されること() {
      // IDがnullまたは空文字のDTOを準備
      StudentDto inputDto = new StudentDto(
          null, // IDなし
          "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
          "Tokyo", 25, "Male", "備考", false
      );

      // 変換実行
      Student result = converter.toEntity(inputDto);

      // 検証
      // 1. 新しいIDがセットされているか
      assertThat(result.getStudentId()).isNotNull();
      // 2. 正しくマッピングされているか
      assertThat(result.getFullName()).isEqualTo("山田 太郎");
    }

    /**
     * {@link StudentConverter#toDto(Student)} が {@link Student} の全フィールドを
     * {@link StudentDto}へ正しくコピーしていることを検証します。
     */
    @Test
    void toDto_Student_正常系_全フィールドが正しくマッピングされIDがエンコードされること() {
      UUID id = UUID.fromString(UUID_STRING);
      // IDありのDTOを準備
      Student input = new Student(
          id,
          "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
          "Tokyo", 25, "Male", "備考", null, null, false
      );

      StudentDto dto = converter.toDto(input);

      // --- Then (検証) ---
      // DTO 内容の検証
      // 1. IDが正しくエンコードされているか
      assertThat(dto.getStudentId()).isEqualTo(UUID_STRING);
      // 2. 他のフィールドが正しくマッピングされているか
      assertThat(dto.getFullName()).isEqualTo("山田 太郎");
      assertThat(dto.getFurigana()).isEqualTo("ヤマダ タロウ");
      assertThat(dto.getNickname()).isEqualTo("Taro");
      assertThat(dto.getEmail()).isEqualTo("taro@example.com");
      assertThat(dto.getLocation()).isEqualTo("Tokyo");
      assertThat(dto.getAge()).isEqualTo(25);
      assertThat(dto.getGender()).isEqualTo("Male");
      assertThat(dto.getRemarks()).isEqualTo("備考");
      assertThat(dto.getDeleted()).isFalse();
    }

    /**
     * {@link StudentConverter#toEntity(StudentCourseDto, String)} において、 コース ID
     * が未指定（null）の場合、新規採番された ID が利用されることを検証します。
     */
    @Test
    void toEntity_StudentCourseDto_CourseIDなし_StudentCourseが新規IDで生成されること() {
      LocalDate start = LocalDate.of(2025, 4, 1);
      LocalDate end = LocalDate.of(2025, 9, 30);

      // --- Given (入力データの準備) ---
      // Course IDが null の入力 DTO を準備
      StudentCourseDto inputDto = new StudentCourseDto(
          null, // ★ CourseId を null に設定
          "Javaコース",
          start,
          end
      );

      // --- When (変換実行) ---
      // パスから渡ってきた想定の studentId（UUID文字列）
      String studentIdString = UUID_STRING;
      UUID studentId = UUID.fromString(studentIdString);

      StudentCourse result = converter.toEntity(inputDto, studentIdString);

      assertThat(result.getCourseId()).isNotNull();           // ランダム採番されている
      assertThat(result.getStudentId()).isEqualTo(studentId); // 正しく紐づいている
      assertThat(result.getCourseName()).isEqualTo("Javaコース");
      assertThat(result.getStartDate()).isEqualTo(start);
      assertThat(result.getEndDate()).isEqualTo(end);
    }

    /**
     * {@link StudentConverter#toEntityList(List, java.util.UUID)} において、 各 {@link StudentCourseDto}
     * に既存のコース ID が指定されている場合、 それぞれが正しくデコードされて {@link StudentCourse} に反映されることを検証します。
     */
    @Test
    void toEntityList_StudentCourseDto_CourseIDあり_既存IDが正しく使用されること() {
      LocalDate start = LocalDate.of(2025, 4, 1);

      StudentCourseDto dto1 = new StudentCourseDto(
          UUID_STRING,
          "Javaコース",
          start,
          start.plusMonths(6)
      );
      StudentCourseDto dto2 = new StudentCourseDto(
          UUID_STRING_B,
          "SQLコース",
          start,
          start.plusMonths(3)
      );

      UUID studentId = UUID.randomUUID();

      List<StudentCourse> result = converter.toEntityList(List.of(dto1, dto2), studentId);

      assertThat(result).hasSize(2);

      StudentCourse courseJava = result.stream()
          .filter(c -> c.getCourseName().equals("Javaコース"))
          .findFirst()
          .orElseThrow();
      StudentCourse courseSql = result.stream()
          .filter(c -> c.getCourseName().equals("SQLコース"))
          .findFirst()
          .orElseThrow();

      assertThat(courseJava.getCourseId()).isEqualTo(UUID.fromString(UUID_STRING));
      assertThat(courseSql.getCourseId()).isEqualTo(UUID.fromString(UUID_STRING_B));

      assertThat(courseJava.getStudentId()).isEqualTo(studentId);
      assertThat(courseSql.getStudentId()).isEqualTo(studentId);
    }

    /**
     * コース DTO を 2 件生成するヘルパーメソッド。
     *
     * @return 固定 ID／コース名を持つ {@link StudentCourseDto} のリスト
     */
    private List<StudentCourseDto> getStudentCourseDtos() {
      LocalDate start = LocalDate.of(2025, 4, 1);

      // 2つのコースDTO（どちらも CourseId が指定されている）
      StudentCourseDto dto1 = new StudentCourseDto(
          UUID_STRING,          // ★ 既存の CourseId（UUID）
          "Javaコース",
          start,
          start.plusMonths(6)
      );
      StudentCourseDto dto2 = new StudentCourseDto(
          UUID_STRING_B,        // ★ 別の CourseId（UUID）
          "SQLコース",
          start,
          start.plusMonths(3)
      );
      return List.of(dto1, dto2);
    }

    /**
     * コース ID が未指定の DTO を渡した場合、新規 ID採番が行われることを検証します。
     */
    @Test
    void toEntityList_StudentCourseDto_CourseIDなし_StudentCourseが新規IDで生成されること() {
      // --- Given ---
      UUID studentId = UUID.fromString(UUID_STRING);

      LocalDate start = LocalDate.of(2025, 4, 1);
      LocalDate end = LocalDate.of(2025, 9, 30);

      // CourseId が null の DTO を1件だけ用意
      StudentCourseDto dto = new StudentCourseDto(
          null,                 // ★ CourseId なし
          "Javaコース",
          start,
          end
      );
      List<StudentCourseDto> dtoList = List.of(dto);

      // --- When ---
      List<StudentCourse> result = converter.toEntityList(dtoList, studentId);

      // --- Then ---
      assertThat(result).hasSize(1);
      StudentCourse course = result.get(0);

      // 1. 新しいIDが「ちゃんと採番されている」こと（null でない）
      assertThat(course.getCourseId()).isNotNull();
      // （必要なら「パラメータで渡した studentId がそのままセットされているか」も確認）
      assertThat(course.getStudentId()).isEqualTo(studentId);
      // 2. 他のフィールドの項目移送
      assertThat(course.getCourseName()).isEqualTo("Javaコース");
      assertThat(course.getStartDate()).isEqualTo(start);
      assertThat(course.getEndDate()).isEqualTo(end);
    }

    // ------------------------------------------------------------
//  リスト/集約変換メソッドのテスト
// ------------------------------------------------------------

    /**
     * 受講生・コース一覧からの集約生成および 部分更新マージ処理を検証するテストグループ。
     */
    @Nested
    class AggregationConversionTest {

      /**
       * {@link StudentConverter#toDetailDtoList(List, List)} が、 学生とコースを学生 ID で正しくグルーピングし、 期待どおりの
       * {@link StudentDetailDto} 一覧を生成することを検証します。
       */
      @Test
      void toDetailDtoList_正常系_学生とコースが正しく紐づけられDTOリストに変換されること() {
        // StudentエンティティとStudentCourseエンティティのリストを用意し、
        // StudentIdでグルーピングされることを確認

        // --- Given (入力データの準備) ---
        LocalDate S = LocalDate.of(2025, 4, 1);

        UUID studentIdA = uuid; // setUp で UUID_STRING から生成済み
        UUID studentIdB = UUID.fromString(UUID_STRING_B);

        Student studentA = new Student(
            studentIdA,
            "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
            "Tokyo", 25, "Male", "備考", null, null, null
        );

        // 2. 学生B (ID: FIXED_UUID_BYTES_B / UUID: FIXED_UUID_STRING_B)
        Student studentB = new Student(
            studentIdB, "田中 花子", "タナカ ハナコ", "Hana",
            "hana@example.com", "Osaka", 30, "Female", "備考", null, null, null
        );

        // 3. コースA (学生Aに紐づくコース)
        StudentCourse courseA1 = new StudentCourse(
            // コースID自体の値は本テストの関心外なので、ゼロ埋め16バイトで十分
            UUID.randomUUID(), studentIdA, "Javaコース",
            S, S.plusMonths(6), null
        );

        // 4. コースB (学生Bに紐づくコース)
        StudentCourse courseB1 = new StudentCourse(
            UUID.randomUUID(), studentIdB, "Pythonコース",
            S, S.plusMonths(3), null
        );
        StudentCourse courseB2 = new StudentCourse(
            UUID.randomUUID(), studentIdB, "SQLコース",
            S, S.plusMonths(1), null
        );

        // 入力リストの作成
        List<Student> students = List.of(studentA, studentB);
        List<StudentCourse> courses = List.of(courseA1, courseB1, courseB2);

        // --- When (変換実行) ---
        List<StudentDetailDto> result =
            converter.toDetailDtoList(students, courses);

        // --- Then (検証) ---
        // 1. DTOリストのサイズが学生の数と一致すること
        assertThat(result).hasSize(2);

        // 2. 学生AのDTOを確認 (リストの最初の要素と仮定)
        StudentDetailDto dtoA = result.stream()
            .filter(d -> d.getStudent().getFullName().equals("山田 太郎"))
            .findFirst().orElseThrow();
        assertThat(dtoA.getStudent().getStudentId()).isEqualTo(UUID_STRING);
        assertThat(dtoA.getCourses()).hasSize(1); // Javaコースのみ

        // 3. 学生BのDTOを確認 (リストの2番目の要素と仮定)
        StudentDetailDto dtoB = result.stream()
            .filter(d -> d.getStudent().getFullName().equals("田中 花子"))
            .findFirst().orElseThrow();
        assertThat(dtoB.getStudent().getStudentId()).isEqualTo(UUID_STRING_B);
        assertThat(dtoB.getCourses()).hasSize(2); // PythonとSQLの2コース

        // 4. コース名が正しく含まれていることを確認（学生B）
        List<String> NamesB = dtoB.getCourses().stream()
            .map(StudentCourseDto::getCourseName)
            .toList();
        assertThat(NamesB).containsExactlyInAnyOrder("Pythonコース", "SQLコース");
      }

      /**
       * {@link StudentConverter#toDetailDtoList(List, List)} において、 コースに紐づかない学生が存在する場合でも、
       * その学生がコース一覧空の {@link StudentDetailDto} として 正しく含まれることを検証します。
       */
      @Test
      void toDetailDtoList_正常系_紐づくコースがない学生も正しくDTOに含まれること() {
        // コースリストが空のケースをテスト

        // --- Given (入力データの準備) ---
        LocalDate S = LocalDate.of(2025, 4, 1);
        UUID studentIdA = uuid; // setUp で UUID_STRING から生成済み
        UUID studentIdB = UUID.fromString(UUID_STRING_B);

        Student studentA = new Student(
            studentIdA,
            "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
            "Tokyo", 25, "Male", "備考", null, null, null
        );
        // 2. 学生B (ID: FIXED_UUID_BYTES_B / UUID: FIXED_UUID_STRING_B)
        Student studentB = new Student(
            studentIdB, "田中 花子", "タナカ ハナコ", "Hana",
            "hana@example.com", "Osaka", 30, "Female", "備考", null, null, null
        );
        // 3. コースA (学生Aに紐づくコース)
        StudentCourse courseA1 = new StudentCourse(
            UUID.randomUUID(),             // courseId 適当でOK
            studentIdA, "Javaコース",
            S, S.plusMonths(6), null
        );

        // 入力リストの作成
        List<Student> students = List.of(studentA, studentB);
        List<StudentCourse> courses = List.of(courseA1); // コースA1のみ

        // --- When (変換実行) ---
        List<StudentDetailDto> result =
            converter.toDetailDtoList(students, courses);

        // --- Then (検証) ---
        // 1. DTOリストのサイズが学生の数と一致すること
        assertThat(result).hasSize(2);

        // 2. 学生AのDTOを確認 (リストの最初の要素と仮定)
        StudentDetailDto dtoA = result.stream()
            .filter(d -> d.getStudent().getFullName().equals("山田 太郎"))
            .findFirst().orElseThrow();
        assertThat(dtoA.getStudent().getStudentId()).isEqualTo(UUID_STRING);
        assertThat(dtoA.getCourses()).hasSize(1); // Javaコースのみ

        // 3. 学生BのDTOを確認 (リストの2番目の要素と仮定)
        StudentDetailDto dtoB = result.stream()
            .filter(d -> d.getStudent().getFullName().equals("田中 花子"))
            .findFirst().orElseThrow();
        assertThat(dtoB.getStudent().getStudentId()).isEqualTo(UUID_STRING_B);
        assertThat(dtoB.getCourses()).isEmpty();
      }

      /**
       * {@link StudentConverter#mergeStudent(Student, Student)} が、 部分更新用エンティティ中の「null
       * でないフィールドのみ」を既存エンティティへ上書きすることを検証します。
       */
      @Test
      void mergeStudent_部分更新_Nullでないフィールドのみが既存データに上書きされること() {
        // mergeStudent(Student existing, Student update) のテスト
        // 既存のデータ（DBから取得した想定）
        Student existing = new Student(
            uuid,
            "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
            "Tokyo", 25, "Male", "元の備考", null, null, null
        );

        // 部分更新用のデータ（リクエストボディの想定）
        Student update = new Student(
            null, // IDはマージ対象外
            "田中 花子", // 氏名は更新
            null, // フリガナはnullなのでスキップ
            "Hana", // ニックネームは更新
            null, // Emailはnullなのでスキップ
            "Osaka", // Locationは更新
            30, // Ageは更新
            null, // Genderはnullなのでスキップ
            "緊急連絡事項", // 備考は更新
            null, null, null // その他のフィールドもnull
        );

        // 実行
        converter.mergeStudent(existing, update);

        // 検証
        // 1. 更新されたフィールドの確認
        assertThat(existing.getFullName()).isEqualTo("田中 花子");
        assertThat(existing.getNickname()).isEqualTo("Hana");
        assertThat(existing.getLocation()).isEqualTo("Osaka");
        assertThat(existing.getAge()).isEqualTo(30);
        assertThat(existing.getRemarks()).isEqualTo("緊急連絡事項");

        // 2. nullのためスキップされ、元の値を維持したフィールドの確認
        assertThat(existing.getFurigana()).isEqualTo("ヤマダ タロウ"); // スキップ
        assertThat(existing.getEmail()).isEqualTo("taro@example.com"); // スキップ
        assertThat(existing.getGender()).isEqualTo("Male"); // スキップ
      }
    }
  }
}
