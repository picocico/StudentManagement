package raisetech.student.management.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentCourseDto;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentDto;
import raisetech.student.management.exception.InvalidIdFormatException;

@ExtendWith(MockitoExtension.class)
class StudentConverterTest {

  // テスト対象クラス（SUT: System Under Test）。モックを注入
  @InjectMocks
  private StudentConverter converter;

  private static String b64(byte[] b) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  // テスト全体で使用する固定データ（Base64, バイト配列など）
  // 通常はUUIDUtilのモックと組み合わせて使用
  private final byte[] FIXED_UUID_BYTES = new byte[]{
      (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
      (byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0,
      (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
      (byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0
  }; // 16バイトの固定値
  private final String FIXED_BASE64_ID = b64(FIXED_UUID_BYTES);

  private final byte[] NEW_RANDOM_BYTES = new byte[]{
      (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
      (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08,
      (byte) 0x09, (byte) 0x0A, (byte) 0x0B, (byte) 0x0C,
      (byte) 0x0D, (byte) 0x0E, (byte) 0x0F, (byte) 0x10
  };// 新規採番用の固定値

  // 学生B 用の固定ID
  private final byte[] FIXED_UUID_BYTES_B = new byte[]{
      (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd,
      (byte) 0xee, (byte) 0xff, (byte) 0x11, (byte) 0x22,
      (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd,
      (byte) 0xee, (byte) 0xff, (byte) 0x11, (byte) 0x22
  };
  private final String FIXED_BASE64_ID_B = b64(FIXED_UUID_BYTES_B); // 仮のBase64 ID B

  // ------------------------------------------------------------
// ID変換メソッドのテスト
// ------------------------------------------------------------
  @Nested
  class IdConversionTest {

    @Test
    void encodeBase64_正常系_16バイトのUUIDバイト配列を正しくエンコードできること() {
      // テストコードを実装 (FIXED_UUID_BYTES, FIXED_BASE64_IDを使用)
      // 固定値の16バイトのUUIDデータ
      // 期待されるURL-safe Base64文字列（paddingなし）
      String result = converter.encodeBase64(FIXED_UUID_BYTES);
      // 期待値と結果が完全に一致することを確認
      // FIXED_UUID_BYTESのBase64エンコード値
      assertThat(result).isEqualTo(FIXED_BASE64_ID);
    }

    @Test
    void encodeBase64_異常系_16バイト以外の長さが入力された場合に例外が発生すること() {
      // テストコードを実装 (IllegalArgumentException)
      // 不正な長さのデータ（例: 4バイト）
      byte[] invalidLengthBytes = new byte[]{0x01, 0x02, 0x03, 0x04};

      // 特定の例外（IllegalArgumentException）がスローされることを確認
      assertThatThrownBy(() -> converter.encodeBase64(invalidLengthBytes))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("UUIDの形式が不正です");
    }

    @Test
    void decodeBase64ToBytes_正常系_有効なBase64文字列を正しくバイト配列にデコードできること() {
      // テストコードを実装
      // 期待される元の16バイトのデータ
      byte[] resultBytes = converter.decodeBase64ToBytes(FIXED_BASE64_ID);
      // バイト配列の内容が一致することを確認
      assertThat(resultBytes).containsExactly(FIXED_UUID_BYTES);
    }

    @Test
    void decodeBase64ToBytes_異常系_不正なBase64が入力された場合にInvalidIdFormatExceptionがスローされること() {
      // テストコードを実装 (InvalidIdFormatException, 「（Base64）」)
      String invalid = FIXED_BASE64_ID.substring(0, FIXED_BASE64_ID.length() - 1) + "!";
      // InvalidIdFormatExceptionがスローされ、かつメッセージに「（Base64）」を含むことを確認
      assertThatThrownBy(() -> converter.decodeBase64ToBytes(invalid))
          .isInstanceOf(InvalidIdFormatException.class)
          .hasMessageContaining("（Base64）");
    }

    @Test
    void decodeIdOrThrow_異常系_Base64デコード後に許容文字外を含む場合に例外がスローされること() {
      // テストコードを実装 (InvalidIdFormatException, 「（UUID）」)
      // 1. Base64デコードは成功するが、許容文字外（#など）を含む生データを準備
      String illegalTextId = "Test#ID_01";
      // 2. その生データをBase64エンコードし、入力文字列とする
      //    （Java標準のURL-safe, paddingなしでエンコード）
      String validBase64 = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(illegalTextId.getBytes(StandardCharsets.UTF_8));
      // validBase64 は例えば "VGVzdCM=?" のような値になるはず（正しくデコードできる形式）

      // --- 実行と検証 ---
      // 期待されるのは、デコード後のパターンチェック失敗による「（UUID）」メッセージの例外
      assertThatThrownBy(() ->
          // 例外をスローするメソッド呼び出しのみをラムダ式に入れる
          converter.decodeIdOrThrow(validBase64))
          // ラムダ式と assertThatThrownBy の引数が終了する！
          .isInstanceOf(InvalidIdFormatException.class)
          // ここが重要：パターンチェック失敗（UUID相当の不正と見なす）のメッセージを確認
          .hasMessageContaining("（UUID）");
    }
  }

  // ------------------------------------------------------------
//　DTO ⇔ エンティティ 変換メソッドのテスト
// ------------------------------------------------------------
  @Nested
  class DtoEntityConversionTest {

    @Test
    void toEntity_StudentDto_IDあり_全フィールドが正しくマッピングされIDがデコードされること() {
      // SpyとdoReturn(FIXED_UUID_BYTES).when(spy).decodeBase64(anyString()) を使用
      // 入力DTOの準備
      StudentDto inputDto = new StudentDto(
          FIXED_BASE64_ID,
          "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
          "Tokyo", 25, "Male", "備考", false
      );

      // decodeBase64()の戻り値をモック（内部依存のテスト）
      // decodeBase64ToBytesの実装をテストするため、decodeBase64(String)の動作を再現
      // MockitoでdecodeBase64ToBytesをスパイ/モックして、FIXED_UUID_BYTESを返すように設定

      // StudentConverterをSpyとして使う（decodeBase64メソッドを呼ぶため）
      StudentConverter spy = Mockito.spy(converter);
      doReturn(FIXED_UUID_BYTES).when(spy).decodeBase64(FIXED_BASE64_ID);

      // 変換実行
      Student result = spy.toEntity(inputDto);

      // 検証
      // 1. IDが正しくデコードされているか
      assertThat(result.getStudentId()).containsExactly(FIXED_UUID_BYTES);
      // 2. 他のフィールドが正しくマッピングされているか
      assertThat(result.getFullName()).isEqualTo("山田 太郎");
      assertThat(result.getAge()).isEqualTo(25);
      assertThat(result.getDeleted()).isFalse();
    }

    @Test
    void toEntity_StudentDto_IDなし_新規にランダムIDが生成されること() {
      // SpyとdoReturn(NEW_RANDOM_BYTES).when(spy).generateRandomBytes() を使用
      // IDがnullまたは空文字のDTOを準備
      StudentDto inputDto = new StudentDto(
          null, // IDなし
          "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
          "Tokyo", 25, "Male", "備考", false
      );

      // StudentConverterをSpyとして使う
      StudentConverter spy = Mockito.spy(converter);
      doReturn(NEW_RANDOM_BYTES).when(spy).generateRandomBytes();

      // 変換実行
      Student result = spy.toEntity(inputDto);

      // 検証
      // 1. 新しいIDがセットされているか
      assertThat(result.getStudentId()).containsExactly(NEW_RANDOM_BYTES);
      // 2. generateRandomBytes()が一度だけ呼ばれたことを確認
      verify(spy, times(1)).generateRandomBytes();
    }

    @Test
    void toDto_Student_正常系_全フィールドが正しくマッピングされIDがエンコードされること() {
      // SpyとdoReturn(FIXED_BASE64_ID).when(spy).encodeBase64(any()) を使用
      Student input = new Student(
          FIXED_UUID_BYTES,
          "山田 太郎", "ヤマダ タロウ", "Taro", "taro@example.com",
          "Tokyo", 25, "Male", "備考", null, null, false
      );

      // --- Mocking (encodeBase64の動作を定義) ---
      // 内部で呼ばれる encodeBase64(FIXED_UUID_BYTES) が FIXED_BASE64_ID を返すように設定
      StudentConverter spy = Mockito.spy(new StudentConverter());

      // --- When (変換実行) ---
      // toDto を実行し、結果を StudentDto で受け取る
      StudentDto dto = spy.toDto(input); // ★ Spy 経由で呼ぶ

      // --- Then (検証) ---
      // encodeBase64 に渡された引数をキャプチャして検証
      ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
      verify(spy, times(1)).encodeBase64(captor.capture()); // ★ capture は verify の引数で使う
      assertThat(captor.getValue()).containsExactly(FIXED_UUID_BYTES);

      // DTO 内容の検証
      // 1. IDが正しくエンコードされているか
      assertThat(dto.getStudentId()).isEqualTo(FIXED_BASE64_ID);
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

    @Test
    void toDto_Student_異常系_ID長が16バイトでない場合にIllegalArgumentExceptionがスローされること() {
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

      // toDtoメソッドは内部でencodeBase64を呼び出し、ID長が16バイトでないため例外が発生する
      assertThatThrownBy(() -> converter.toDto(input))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("UUIDの形式が不正です");
    }

    @Test
    void toEntity_StudentCourseDto_CourseIDなし_StudentCourseが新規IDで生成されること() {
      // Course IDの新規採番ロジックをテスト
      // Student IDのBase64文字列 (紐付け用)
      final String studentIdBase64 = FIXED_BASE64_ID;

      // --- Given (入力データの準備) ---
      // Course IDが null の入力 DTO を準備
      StudentCourseDto inputDto = new StudentCourseDto(
          null, // ★ CourseId を null に設定
          "Javaコース", LocalDate.of(2025, 4, 1), LocalDate.of(2025, 9, 30)
          // LocalDate オブジェクトとして渡す
      );

      // --- Mocking (新規ID生成の動作を定義) ---
      // StudentConverterをSpyとして使う
      StudentConverter spy = Mockito.spy(converter);
      // generateRandomBytes()が新規ID (NEW_RANDOM_BYTES) を返すように設定
      doReturn(NEW_RANDOM_BYTES).when(spy).generateRandomBytes();

      // ★ toEntity内部で decodeBase64 が呼ばれるため、Student IDの復号結果も定義
      byte[] fixedStudentBytes = new byte[16]; // 紐付け用 学生IDのバイト配列
      doReturn(fixedStudentBytes).when(spy).decodeBase64(studentIdBase64);

      // --- When (変換実行) ---
      // 正しいメソッドと引数 (DTO, Base64 Student ID) で呼び出し
      // 戻り値の型は StudentCourse (エンティティ)
      StudentCourse result = spy.toEntity(inputDto, studentIdBase64);

      // --- Then (検証) ---
      // 1. 新しいIDがセットされているか
      // resultEntity は StudentCourse (エンティティ) なので、IDはバイト配列
      // resultEntity.getCourseId() は byte[] 型なので isEqualTo を使用
      assertThat(result.getCourseId()).containsExactly(NEW_RANDOM_BYTES);
      // 2. 他のフィールドが正しくマッピングされているか
      assertThat(result.getCourseName()).isEqualTo("Javaコース");
      // 3. Student IDが正しく紐づいているか
      // resultEntity は StudentCourse (エンティティ) なので getStudentId() を使用
      assertThat(result.getStudentId()).containsExactly(fixedStudentBytes);
      // 4. generateRandomBytes()が一度だけ呼ばれたことを確認
      verify(spy, times(1)).generateRandomBytes();
    }
  }

  // ------------------------------------------------------------
//  リスト/集約変換メソッドのテスト
// ------------------------------------------------------------
  @Nested
  class AggregationConversionTest {

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

      // 2. 学生B (ID: FIXED_UUID_BYTES_B / Base64: FIXED_BASE64_ID_B)
      Student studentB = new Student(
          FIXED_UUID_BYTES_B, "田中 花子", "タナカ ハナコ", "Hana",
          "hana@example.com", "Osaka", 30, "Female", "備考", null, null, null
      );

      // 3. コースA (学生Aに紐づくコース)
      StudentCourse courseA1 = new StudentCourse(
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
      assertThat(dtoA.getStudent().getStudentId()).isEqualTo(FIXED_BASE64_ID);
      assertThat(dtoA.getCourses()).hasSize(1); // Javaコースのみ

      // 3. 学生BのDTOを確認 (リストの2番目の要素と仮定)
      StudentDetailDto dtoB = result.stream()
          .filter(d -> d.getStudent().getFullName().equals("田中 花子"))
          .findFirst().orElseThrow();
      assertThat(dtoB.getStudent().getStudentId()).isEqualTo(FIXED_BASE64_ID_B);
      assertThat(dtoB.getCourses()).hasSize(2); // PythonとSQLの2コース

      // 4. コース名が正しく含まれていることを確認（学生B）
      List<String> NamesB = dtoB.getCourses().stream()
          .map(StudentCourseDto::getCourseName)
          .toList();
      assertThat(NamesB).containsExactlyInAnyOrder("Pythonコース", "SQLコース");
    }

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
      // 2. 学生B (ID: FIXED_UUID_BYTES_B / Base64: FIXED_BASE64_ID_B)
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
      assertThat(dtoA.getStudent().getStudentId()).isEqualTo(FIXED_BASE64_ID);
      assertThat(dtoA.getCourses()).hasSize(1); // Javaコースのみ

      // 3. 学生BのDTOを確認 (リストの2番目の要素と仮定)
      StudentDetailDto dtoB = result.stream()
          .filter(d -> d.getStudent().getFullName().equals("田中 花子"))
          .findFirst().orElseThrow();
      assertThat(dtoB.getStudent().getStudentId()).isEqualTo(FIXED_BASE64_ID_B);
      assertThat(dtoB.getCourses()).isEmpty();
    }

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

