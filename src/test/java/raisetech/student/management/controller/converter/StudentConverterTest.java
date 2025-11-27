package raisetech.student.management.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentCourseDto;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentDto;
import raisetech.student.management.exception.InvalidIdFormatException;
import raisetech.student.management.util.IdCodec;

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
 * <p>{@link IdCodec} はモック化し、UUID の具体的な値や UUID文字列 実装詳細に依存しない形で
 * コンバータの責務のみを検証します。
 */
@ExtendWith(MockitoExtension.class)
class StudentConverterTest {

  /**
   * ID 変換処理を委譲するユーティリティのモック。
   *
   * <p>UUID 16 バイトと UUID 文字列の相互変換ロジックは本クラスの関心外とし、
   * その戻り値／例外を固定することで {@link StudentConverter} の挙動を検証します。
   */
  @Mock
  IdCodec idCodec;

  /**
   * テスト対象となるコンバータ。
   *
   * <p>{@link IdCodec} モックが自動的にインジェクションされます。
   */
  @InjectMocks
  private StudentConverter converter;

  /**
   * テスト共通で利用する 「UUID の生バイト表現」を示す16 バイト固定 ID（学生 A 用）。
   *
   * <p>値そのもの（ビットパターン）はテストの関心外であり、
   * 「常に16バイトの UUID/BINARY(16) である」ことだけを保証したいケースで利用します。
   */
  private final byte[] FIXED_UUID_BYTES = new byte[]{
      (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
      (byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0,
      (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
      (byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0
  };

  // テスト用の固定UUID文字列
  private static final String FIXED_UUID_STRING = "123e4567-e89b-12d3-a456-426614174000";

  /**
   * 新規採番を想定した 「UUID の生バイト表現」を示す16 バイトの固定 ID。
   *
   * <p>{@link IdCodec#generateNewIdBytes()} の戻り値として利用し、
   * 「ランダムだが 16 バイトである」という前提をテストに与えます。
   */
  private final byte[] NEW_RANDOM_BYTES = new byte[]{
      (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
      (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08,
      (byte) 0x09, (byte) 0x0A, (byte) 0x0B, (byte) 0x0C,
      (byte) 0x0D, (byte) 0x0E, (byte) 0x0F, (byte) 0x10
  };

  /**
   * テスト内で使用する「UUID の生バイト表現」を示す学生 B 用の 16 バイト固定 ID。
   */
  private final byte[] FIXED_UUID_BYTES_B = new byte[]{
      (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd,
      (byte) 0xee, (byte) 0xff, (byte) 0x11, (byte) 0x22,
      (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd,
      (byte) 0xee, (byte) 0xff, (byte) 0x11, (byte) 0x22
  };

  private static final String FIXED_UUID_STRING_B = "123e4567-e89b-12d3-a456-426614174001";

  // ------------------------------------------------------------
  // ID変換メソッドのテスト
  // ------------------------------------------------------------

  /**
   * ID 変換系メソッド（UUID ⇔ byte[]、文字列 ID デコード）のテストグループ。
   */
  @Nested
  class IdConversionTest {

    /**
     * {@link StudentConverter#encodeUuidString(byte[])} が 16 バイトの UUID バイト配列を正しく UUID
     * 文字列へ変換し、{@link IdCodec#encodeId(byte[])} へ委譲されていることを検証します。
     */
    @Test
    void encodeUuidString_正常系_16バイトのUUIDバイト配列を正しくエンコードできること() {
      // テストコードを実装 (FIXED_UUID_BYTES, FIXED_UUID_STRINGを使用)
      // 固定値の16バイトのUUIDデータ
      // 期待される UUID 文字列
      // IdCodec に委譲されること＋戻り値がそのまま返ることを確認
      when(idCodec.encodeId(FIXED_UUID_BYTES)).thenReturn(FIXED_UUID_STRING);

      String result = converter.encodeUuidString(FIXED_UUID_BYTES);
      // 期待値と結果が完全に一致することを確認
      // FIXED_UUID_BYTESのUUIDエンコード値
      assertThat(result).isEqualTo(FIXED_UUID_STRING);
    }

    /**
     * {@link StudentConverter#encodeUuidString(byte[])} に 16 バイト以外の配列が渡された場合、 内部で利用する
     * {@link IdCodec#encodeId(byte[])} から {@link IllegalArgumentException} が そのまま伝播することを検証します。
     */
    @Test
    void encodeUuidString_異常系_16バイト以外の長さが入力された場合に例外が発生すること() {
      // テストコードを実装 (IllegalArgumentException)
      // 不正な長さのデータ（例: 4バイト）
      byte[] invalidLengthBytes = new byte[]{0x01, 0x02, 0x03, 0x04};

      // 16バイトチェックは IdCodec 側の責務とし、Converter は例外をそのまま伝播する
      when(idCodec.encodeId(invalidLengthBytes))
          .thenThrow(new IllegalArgumentException("UUIDの形式が不正です"));

      // 特定の例外（InvalidIdFormatException）がスローされることを確認
      // （このチェックは Converter 側で行っているので、IdCodec のモックは不要）
      assertThatThrownBy(() -> converter.encodeUuidString(invalidLengthBytes))
          .isInstanceOf(InvalidIdFormatException.class)
          .hasMessageContaining("IDの形式が不正です（UUIDバイト長が不正など）");
    }

    /**
     * {@link StudentConverter#decodeUuidStringToBytesOrThrow(String)} が 正常な UUID 文字列を正しくバイト配列へ復元し、
     * {@link IdCodec#decodeUuidBytesOrThrow(String)} に委譲していることを検証します。
     */
    @Test
    void decodeUuidToBytesOrThrow_正常系_UUID文字列を正しくバイト配列にデコードできること() {
      // テストコードを実装
      String uuidString = FIXED_UUID_STRING; // "123e4567-e89b-12d3-a456-426614174000" など
      when(idCodec.decodeUuidBytesOrThrow(uuidString)).thenReturn(FIXED_UUID_BYTES);

      byte[] resultBytes = converter.decodeUuidStringToBytesOrThrow(uuidString);
      // バイト配列の内容が一致することを確認
      assertThat(resultBytes).containsExactly(FIXED_UUID_BYTES);
    }

    /**
     * {@link StudentConverter# decodeUuidBytesOrThrow(String)} に不正な UUID 文字列が渡された場合、
     * {@link InvalidIdFormatException}（「（UUID）」）がスローされることを検証します。
     */
    @Test
    void decodeUuidToBytesOrThrow_異常系_不正なUUIDが入力された場合にInvalidIdFormatExceptionがスローされること() {
      String invalid = "invalid!!";
      when(idCodec.decodeUuidBytesOrThrow(invalid))
          .thenThrow(new IllegalArgumentException("dummy"));

      assertThatThrownBy(() -> converter.decodeUuidStringToBytesOrThrow(invalid))
          .isInstanceOf(InvalidIdFormatException.class)
          .hasMessageContaining("（UUID）"); // メッセージは実装に合わせて
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
     * {@link Student} へ変換し、 ID デコードを {@link IdCodec#decodeUuidBytesOrThrow(String)}
     * に委譲していることを検証します。
     */
    @Test
    void toEntity_StudentDto_IDあり_全フィールドが正しくマッピングされIDがデコードされること() {
      // IdCodec のモックで ID デコード結果を固定し、項目移送を検証する
      StudentDto inputDto = new StudentDto(
          FIXED_UUID_STRING,
          "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
          "Tokyo", 25, "Male", "備考", false
      );

      // IDデコードは IdCodec に委譲される
      when(idCodec.decodeUuidBytesOrThrow(FIXED_UUID_STRING)).thenReturn(FIXED_UUID_BYTES);

      // 変換実行
      Student result = converter.toEntity(inputDto);

      // 検証
      // 1. IDが正しくデコードされているか
      assertThat(result.getStudentId()).containsExactly(FIXED_UUID_BYTES);
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
     * {@link StudentConverter#toEntity(StudentDto)} において、 ID が未指定の場合（null）のときに
     * {@link IdCodec#generateNewIdBytes()} が呼び出され、 新規 ID が採番されることを検証します。
     */
    @Test
    void toEntity_StudentDto_IDなし_新規にランダムIDが生成されること() {
      // IDがnullまたは空文字のDTOを準備
      StudentDto inputDto = new StudentDto(
          null, // IDなし
          "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
          "Tokyo", 25, "Male", "備考", false
      );

      // 新規ID生成は IdCodec に委譲される
      when(idCodec.generateNewIdBytes()).thenReturn(NEW_RANDOM_BYTES);

      // 変換実行
      Student result = converter.toEntity(inputDto);

      // 検証
      // 1. 新しいIDがセットされているか
      assertThat(result.getStudentId()).containsExactly(NEW_RANDOM_BYTES);
      // 2. 正しくマッピングされているか
      assertThat(result.getFullName()).isEqualTo("山田 太郎");
    }

    /**
     * {@link StudentConverter#toDto(Student)} が {@link Student} の全フィールドを {@link StudentDto}
     * へ正しくコピーし、 ID 部分のエンコードに {@link IdCodec#encodeId(byte[])} を利用していることを検証します。
     */
    @Test
    void toDto_Student_正常系_全フィールドが正しくマッピングされIDがエンコードされること() {
      // IDありのDTOを準備
      Student input = new Student(
          FIXED_UUID_BYTES,
          "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
          "Tokyo", 25, "Male", "備考", null, null, false
      );

      // --- When (変換実行) ---
      // toDto を実行し、結果を StudentDto で受け取る
      when(idCodec.encodeId(FIXED_UUID_BYTES)).thenReturn(FIXED_UUID_STRING);

      StudentDto dto = converter.toDto(input);

      // --- Then (検証) ---
      // DTO 内容の検証
      // 1. IDが正しくエンコードされているか
      assertThat(dto.getStudentId()).isEqualTo(FIXED_UUID_STRING);
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
     * {@link StudentConverter#toDto(Student)} において、学生 ID が 16 バイト未満の場合、 内部で呼び出される
     * {@link IdCodec#encodeId(byte[])} が {@link IllegalArgumentException} を投げ、 それが
     * {@link InvalidIdFormatException} にラップされてコンバータから伝播することを検証します。
     */
    @Test
    void toDto_Student_異常系_ID長が16バイトでない場合にInvalidIdFormatExceptionがスローされること() {
      // 💡 異常系データ: 15バイトのIDを持つバイト配列を作成
      byte[] invalid = new byte[]{
          0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
          0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f // 合計15バイト (16バイト未満)
      };

      // 💡 この不正なIDを持つStudentエンティティを作成
      Student input = new Student(
          invalid, "テスト", "テスト", "Test", "test@test.com",
          "Tokyo", 20, "Male", "備考", null, null, false
      );

      // ★ IdCodec が長さ不正を検知して IllegalArgumentException を投げるようにスタブ
      when(idCodec.encodeId(invalid))
          .thenThrow(new IllegalArgumentException("UUIDの形式が不正です"));

      // toDtoメソッドは内部でencodeIdを呼び出し、ID長が16バイトでないため例外が発生する
      assertThatThrownBy(() -> converter.toDto(input))
          .isInstanceOf(InvalidIdFormatException.class)
          .hasMessageContaining("IDの形式が不正です");

      // ★ ちゃんと IdCodec が呼ばれていることも確認しておくと安心
      verify(idCodec).encodeId(invalid);
    }

    /**
     * {@link StudentConverter#toEntity(StudentCourseDto, String)} において、 コース ID
     * が未指定（null）の場合、新規採番された ID が利用されることを検証します。
     */
    @Test
    void toEntity_StudentCourseDto_CourseIDなし_StudentCourseが新規IDで生成されること() {
      // Course IDの新規採番ロジックをテスト
      // Student IDのUUID文字列 (紐付け用)
      final String uuidString = FIXED_UUID_STRING;

      // --- Given (入力データの準備) ---
      // Course IDが null の入力 DTO を準備
      StudentCourseDto inputDto = new StudentCourseDto(
          null, // ★ CourseId を null に設定
          "Javaコース", LocalDate.of(2025, 4, 1), LocalDate.of(2025, 9, 30)
          // LocalDate オブジェクトとして渡す
      );

      // --- Mocking (新規ID生成の動作を定義) ---
      // 新規Course ID
      when(idCodec.generateNewIdBytes()).thenReturn(NEW_RANDOM_BYTES);

      // Student IDのデコード結果（UUID 16バイトとして扱う）
      byte[] fixedStudentBytes = new byte[16]; // 紐付け用 学生IDのバイト配列
      when(idCodec.decodeUuidBytesOrThrow(uuidString)).thenReturn(fixedStudentBytes);

      // --- When (変換実行) ---
      // 正しいメソッドと引数 (DTO, uuidString) で呼び出し
      // 戻り値の型は StudentCourse (エンティティ)
      StudentCourse result = converter.toEntity(inputDto, uuidString);

      // --- Then (検証) ---
      // 1. 新しいIDがセットされているか
      // resultEntity は StudentCourse (エンティティ) なので、IDはバイト配列
      // resultEntity.getCourseId() は byte[] 型なので isEqualTo を使用
      assertThat(result.getCourseId()).containsExactly(NEW_RANDOM_BYTES);
      // 2. Student IDが正しく紐づいているか
      assertThat(result.getStudentId()).containsExactly(fixedStudentBytes);
      // 3.　他のフィールドが正しくマッピングされているか
      assertThat(result.getCourseName()).isEqualTo("Javaコース");
      assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2025, 4, 1));
      assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2025, 9, 30));
    }

    /**
     * {@link StudentConverter#toEntityList(List, byte[])} において、 各 {@link StudentCourseDto} に既存のコース
     * ID が指定されている場合、 それぞれが正しくデコードされて {@link StudentCourse} に反映されることを検証します。
     */
    @Test
    void toEntityList_StudentCourseDto_CourseIDあり_既存IDが正しくデコードされて使用されること() {
      // --- Given ---
      byte[] studentIdBytes = FIXED_UUID_BYTES; // 紐付け先の受講生ID（UUIDの16バイト）

      List<StudentCourseDto> dtoList = getStudentCourseDtos();

      // IdCodec による CourseId の復号結果をモック
      // UUID → UUID 16バイト
      when(idCodec.decodeUuidBytesOrThrow(FIXED_UUID_STRING)).thenReturn(FIXED_UUID_BYTES);
      when(idCodec.decodeUuidBytesOrThrow(FIXED_UUID_STRING_B)).thenReturn(FIXED_UUID_BYTES_B);

      // --- When ---
      List<StudentCourse> result = converter.toEntityList(dtoList, studentIdBytes);

      // --- Then ---
      assertThat(result).hasSize(2);

      // コース名で取り出して検証（順序にあまり依存したくない場合）
      StudentCourse courseJava = result.stream()
          .filter(c -> c.getCourseName().equals("Javaコース"))
          .findFirst()
          .orElseThrow();

      StudentCourse courseSql = result.stream()
          .filter(c -> c.getCourseName().equals("SQLコース"))
          .findFirst()
          .orElseThrow();

      // 1. CourseId がそれぞれ正しくデコードされていること
      assertThat(courseJava.getCourseId()).containsExactly(FIXED_UUID_BYTES);
      assertThat(courseSql.getCourseId()).containsExactly(FIXED_UUID_BYTES_B);

      // 2. どちらのコースも同じ studentId に紐づいていること
      assertThat(courseJava.getStudentId()).containsExactly(studentIdBytes);
      assertThat(courseSql.getStudentId()).containsExactly(studentIdBytes);

      // 3. 他の項目移送（ここではコース名だけ軽く確認）
      assertThat(courseJava.getCourseName()).isEqualTo("Javaコース");
      assertThat(courseSql.getCourseName()).isEqualTo("SQLコース");
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
          FIXED_UUID_STRING,          // ★ 既存の CourseId（UUID）
          "Javaコース",
          start,
          start.plusMonths(6)
      );
      StudentCourseDto dto2 = new StudentCourseDto(
          FIXED_UUID_STRING_B,        // ★ 別の CourseId（UUID）
          "SQLコース",
          start,
          start.plusMonths(3)
      );
      return List.of(dto1, dto2);
    }

    /**
     * {@link StudentConverter#toEntityList(List, byte[])} において、 コース ID が未指定の DTO を渡した場合、新規 ID
     * 採番が行われることを検証します。
     */
    @Test
    void toEntityList_StudentCourseDto_CourseIDなし_StudentCourseが新規IDで生成されること() {
      // --- Given ---
      byte[] studentIdBytes = FIXED_UUID_BYTES; // 紐付け先の受講生ID（UUIDの16バイト）

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

      // 新規 Course ID は IdCodec の generateNewIdBytes に委譲される
      when(idCodec.generateNewIdBytes()).thenReturn(NEW_RANDOM_BYTES);

      // --- When ---
      List<StudentCourse> result = converter.toEntityList(dtoList, studentIdBytes);

      // --- Then ---
      assertThat(result).hasSize(1);
      StudentCourse course = result.get(0);

      // 1. 新しいIDがセットされていること
      assertThat(course.getCourseId()).containsExactly(NEW_RANDOM_BYTES);
      // 2. 渡した studentId がそのまま紐づいていること
      assertThat(course.getStudentId()).containsExactly(studentIdBytes);
      // 3. 他のフィールドの項目移送
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
        Student studentA = new Student(
            FIXED_UUID_BYTES,
            "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
            "Tokyo", 25, "Male", "備考", null, null, null
        );

        // 2. 学生B (ID: FIXED_UUID_BYTES_B / UUID: FIXED_UUID_STRING_B)
        Student studentB = new Student(
            FIXED_UUID_BYTES_B, "田中 花子", "タナカ ハナコ", "Hana",
            "hana@example.com", "Osaka", 30, "Female", "備考", null, null, null
        );

        // 3. コースA (学生Aに紐づくコース)
        StudentCourse courseA1 = new StudentCourse(
            // コースID自体の値は本テストの関心外なので、ゼロ埋め16バイトで十分
            new byte[16], FIXED_UUID_BYTES, "Javaコース",
            S, S.plusMonths(6), null
        );

        // 4. コースB (学生Bに紐づくコース)
        StudentCourse courseB1 = new StudentCourse(
            new byte[16], FIXED_UUID_BYTES_B, "Pythonコース",
            S, S.plusMonths(3), null
        );
        StudentCourse courseB2 = new StudentCourse(
            new byte[16], FIXED_UUID_BYTES_B, "SQLコース",
            S, S.plusMonths(1), null
        );

        // 入力リストの作成
        List<Student> students = List.of(studentA, studentB);
        List<StudentCourse> courses = List.of(courseA1, courseB1, courseB2);

        // 学生IDのUUID文字列化は IdCodec に委譲される
        when(idCodec.encodeId(FIXED_UUID_BYTES)).thenReturn(FIXED_UUID_STRING);
        when(idCodec.encodeId(FIXED_UUID_BYTES_B)).thenReturn(FIXED_UUID_STRING_B);

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
        assertThat(dtoA.getStudent().getStudentId()).isEqualTo(FIXED_UUID_STRING);
        assertThat(dtoA.getCourses()).hasSize(1); // Javaコースのみ

        // 3. 学生BのDTOを確認 (リストの2番目の要素と仮定)
        StudentDetailDto dtoB = result.stream()
            .filter(d -> d.getStudent().getFullName().equals("田中 花子"))
            .findFirst().orElseThrow();
        assertThat(dtoB.getStudent().getStudentId()).isEqualTo(FIXED_UUID_STRING_B);
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
        Student studentA = new Student(
            FIXED_UUID_BYTES,
            "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
            "Tokyo", 25, "Male", "備考", null, null, null
        );
        // 2. 学生B (ID: FIXED_UUID_BYTES_B / UUID: FIXED_UUID_STRING_B)
        Student studentB = new Student(
            FIXED_UUID_BYTES_B, "田中 花子", "タナカ ハナコ", "Hana",
            "hana@example.com", "Osaka", 30, "Female", "備考", null, null, null
        );
        // 3. コースA (学生Aに紐づくコース)
        StudentCourse courseA1 = new StudentCourse(
            new byte[16], FIXED_UUID_BYTES, "Javaコース",
            S, S.plusMonths(6), null
        );

        // 入力リストの作成
        List<Student> students = List.of(studentA, studentB);
        List<StudentCourse> courses = List.of(courseA1); // コースA1のみ

        when(idCodec.encodeId(any())).thenReturn("IGNORED"); // コースIDなど、テストの関心外
        // そのうえで、「学生ID」だけは上書きして本物の期待値を返す
        when(idCodec.encodeId(FIXED_UUID_BYTES)).thenReturn(FIXED_UUID_STRING);
        when(idCodec.encodeId(FIXED_UUID_BYTES_B)).thenReturn(FIXED_UUID_STRING_B);

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
        assertThat(dtoA.getStudent().getStudentId()).isEqualTo(FIXED_UUID_STRING);
        assertThat(dtoA.getCourses()).hasSize(1); // Javaコースのみ

        // 3. 学生BのDTOを確認 (リストの2番目の要素と仮定)
        StudentDetailDto dtoB = result.stream()
            .filter(d -> d.getStudent().getFullName().equals("田中 花子"))
            .findFirst().orElseThrow();
        assertThat(dtoB.getStudent().getStudentId()).isEqualTo(FIXED_UUID_STRING_B);
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
            FIXED_UUID_BYTES,
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
