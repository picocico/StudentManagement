package raisetech.student.management.controller.converter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.domain.StudentDetail;
import raisetech.student.management.dto.StudentCourseDto;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentDto;
import raisetech.student.management.exception.InvalidIdFormatException;
import raisetech.student.management.util.UUIDUtil;


/**
 * 受講生・コースのエンティティ、DTO、ドメインモデル間の相互変換を担うコンバータ。
 *
 * <h3>ID表現の方針</h3>
 * <ul>
 *   <li>DBの主キーは {@code byte[16]}（UUIDの生バイト）を前提とします。</li>
 *   <li>APIの入出力は URL-safe Base64（paddingなし）文字列を基本とします。</li>
 *   <li>不正な Base64 は {@link InvalidIdFormatException}（「（Base64）」メッセージ）を投げます。</li>
 *   <li>UUIDとして不正な場合は {@link InvalidIdFormatException}（「（UUID）」メッセージ）を投げます。</li>
 * </ul>
 *
 * <p>このクラスのメソッドは、コントローラ／サービス層から再利用できるよう
 * 例外をドメイン指向の {@code InvalidIdFormatException} に揃えています。
 * グローバル例外ハンドラ側でメッセージに応じてエラーコード（E006/E004等）を割り当ててください。</p>
 */
@Component
public class StudentConverter {

  /**
   * 文字列IDとして許容する文字（英数・ドット・アンダースコア・ハイフン）
   */
  private static final Pattern ID_TEXT_PATTERN = Pattern.compile("^[0-9A-Za-z._\\-]+$");

  // ------------------------------------------------------------
  // Base64 エンコード／デコード（UUID 16バイトを前提に堅牢化）
  // ------------------------------------------------------------

  /**
   * URLセーフな Base64 文字列にエンコードします（paddingなし）。
   *
   * <p>エンコード対象は UUID の {@code byte[16]} を前提とし、それ以外の長さは例外とします。
   * これにより、誤ったIDがレスポンスへ出力されることを防止します。</p>
   *
   * @param bytes エンコード対象のバイナリ（通常は UUID の生16バイト）
   * @return URLセーフ Base64（paddingなし）の文字列。{@code bytes} が {@code null} の場合は {@code null}
   * @throws IllegalArgumentException 長さが16バイト以外の場合
   */
  public String encodeBase64(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    if (bytes.length != 16) {
      throw new IllegalArgumentException("UUIDの形式が不正です");
    }
    // UUIDUtil は URL-safe & no padding 前提の実装
    return UUIDUtil.toBase64(bytes); // URL-safe & no padding 前提のユーティリティ
  }

  /**
   * URL-safe Base64 文字列をデコードしてバイト配列に変換します。
   * <p>長さチェックは行いません（用途側で行います）。</p>
   *
   * @param encoded URL-safe Base64（paddingなし）文字列
   * @return 復号後のバイト配列
   * @throws InvalidIdFormatException Base64 として不正な場合（「（Base64）」）
   */
  public byte[] decodeBase64ToBytes(String encoded) {
    try {
      return java.util.Base64.getUrlDecoder().decode(encoded);
    } catch (IllegalArgumentException e) {
      // Base64として不正 → （Base64）
      throw new InvalidIdFormatException("IDの形式が不正です（Base64）", e);
    }
  }

  /**
   * 互換ラッパー：既存コードが呼ぶ {@code decodeBase64(String)} を維持します。 実体は {@link #decodeBase64ToBytes(String)}
   * です。
   *
   * @param encoded URL-safe Base64（paddingなし）文字列
   * @return 復号したバイト配列（長さチェックはしません）
   * @throws InvalidIdFormatException Base64として不正な場合（「（Base64）」）
   */
  public byte[] decodeBase64(String encoded) {
    return decodeBase64ToBytes(encoded);
  }

  /**
   * 文字列ID（英数・ドット・アンダースコア・ハイフンのみ許容）としてデコードします。
   *
   * <p>主にパス変数のような「UUID以外の文字列ID」ケースで使用します。
   * Base64として不正な場合は「（Base64）」、許容文字外を含む場合は「（UUID）」として扱います。</p>
   *
   * @param encoded URL-safe Base64（paddingなし）文字列
   * @return 復号後の文字列ID
   * @throws InvalidIdFormatException Base64不正（「（Base64）」）または文字列IDとして不正（「（UUID）」）
   */
  @SuppressWarnings("unused")
  public String decodeIdOrThrow(String encoded) {
    final byte[] bytes = decodeBase64ToBytes(encoded); // Base64不正はここで例外化
    final String id = new String(bytes, StandardCharsets.UTF_8);

    if (!ID_TEXT_PATTERN.matcher(id).matches()) {
      // 仕様上は文字列IDの想定だが、明らかに不正 → UUID相当の不正に寄せる
      throw new InvalidIdFormatException("IDの形式が不正です（UUID）");
    }
    return id;
  }

  /**
   * UUIDを期待する箇所向けのデコード。
   * <ul>
   *   <li>Base64化された「UUID文字列表現」（36文字）</li>
   *   <li>Base64化された「UUIDの生16バイト」</li>
   * </ul>
   * の両方に対応します。
   *
   * @param encoded URL-safe Base64（paddingなし）文字列
   * @return 復号した {@link UUID}
   * @throws InvalidIdFormatException Base64不正（「（Base64）」）またはUUID不正（「（UUID）」）
   */
  @SuppressWarnings("unused")
  public UUID decodeUuidOrThrow(String encoded) {
    final byte[] bytes = decodeBase64ToBytes(encoded); // Base64不正はここで例外化

    // (a) UUID文字列表現（例: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"）がBase64化されていた場合
    final String asText = new String(bytes, StandardCharsets.UTF_8);
    try {
      return UUID.fromString(asText);
    } catch (IllegalArgumentException ignore) {
      // (b) UUIDの生16バイトがBase64化されていた場合
      if (bytes.length == 16) {
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
      }
      // どちらでもない
      throw new InvalidIdFormatException("IDの形式が不正です（UUID）");
    }
  }

  /**
   * ランダムなUUIDを生成し、その生16バイトを返します。
   *
   * @return ランダムUUIDの {@code byte[16]}
   */
  public byte[] generateRandomBytes() {
    return UUIDUtil.fromUUID(UUID.randomUUID());
  }

  // ------------------------------------------------------------
  // Student 変換
  // ------------------------------------------------------------

  /**
   * {@link StudentDto} から {@link Student} エンティティに変換します。
   * <p>
   * {@code dto.studentId} が未指定（null または空文字）の場合は、新規にランダムUUIDを採番し、 その生16バイトをセットします。
   * </p>
   *
   * @param dto 受講生DTO（IDは Base64 文字列）
   * @return 受講生エンティティ（IDは生16バイト）
   * @throws InvalidIdFormatException Base64が不正な場合（「（Base64）」）
   */
  public Student toEntity(StudentDto dto) {
    byte[] studentId = Optional.ofNullable(dto.getStudentId())
        .filter(id -> !id.isBlank())
        .map(this::decodeBase64)
        .orElseGet(this::generateRandomBytes);

    return new Student(
        studentId,
        dto.getFullName(),
        dto.getFurigana(),
        dto.getNickname(),
        dto.getEmail(),
        dto.getLocation(),
        dto.getAge(),
        dto.getGender(),
        dto.getRemarks(),
        null, // createdAt（DB側で生成）
        null, // updatedAt（DB側で更新）
        dto.getDeleted()
    );
  }

  /**
   * {@link Student} エンティティから {@link StudentDto} に変換します。
   *
   * @param entity 受講生エンティティ
   * @return 受講生DTO（IDは Base64 文字列）
   * @throws IllegalArgumentException IDバイト長が16以外の場合
   */
  public StudentDto toDto(Student entity) {
    return new StudentDto(
        encodeBase64(entity.getStudentId()),
        entity.getFullName(),
        entity.getFurigana(),
        entity.getNickname(),
        entity.getEmail(),
        entity.getLocation(),
        entity.getAge(),
        entity.getGender(),
        entity.getRemarks(),
        entity.getDeleted()
    );
  }

  // ------------------------------------------------------------
  // StudentCourse 変換
  // ------------------------------------------------------------

  /**
   * {@link StudentCourseDto} から {@link StudentCourse} エンティティに変換します。
   *
   * <p>{@code dto.courseId} が未指定なら新規採番し、{@code studentIdBase64} は必ず Base64 復号して紐付けます。</p>
   *
   * @param dto             コースDTO（IDは Base64 文字列）
   * @param studentIdBase64 受講生ID（Base64 文字列）
   * @return コースエンティティ（IDは生16バイト）
   * @throws InvalidIdFormatException Base64が不正な場合（「（Base64）」）
   */
  @SuppressWarnings("unused")
  public StudentCourse toEntity(StudentCourseDto dto, String studentIdBase64) {
    byte[] courseId = Optional.ofNullable(dto.getCourseId())
        .filter(id -> !id.isBlank())
        .map(this::decodeBase64)
        .orElseGet(this::generateRandomBytes);

    return new StudentCourse(
        courseId,
        decodeBase64(studentIdBase64),
        dto.getCourseName(),
        dto.getStartDate(),
        dto.getEndDate(),
        null // createdAt（DB側）
    );
  }

  /**
   * {@link StudentCourse} エンティティから {@link StudentCourseDto} に変換します。
   *
   * @param entity コースエンティティ
   * @return コースDTO（IDは Base64 文字列）
   * @throws IllegalArgumentException IDバイト長が16以外の場合
   */
  public StudentCourseDto toDto(StudentCourse entity) {
    return new StudentCourseDto(
        encodeBase64(entity.getCourseId()),
        entity.getCourseName(),
        entity.getStartDate(),
        entity.getEndDate()
    );
  }

  /**
   * コースDTOのリストをエンティティリストに変換します。
   * <p>各 {@code dto.courseId} が未指定なら新規採番し、{@code studentId} を紐付けます。</p>
   *
   * @param dtoList   コースDTO一覧
   * @param studentId 紐付け先の受講生ID（生16バイト）
   * @return コースエンティティ一覧
   * @throws InvalidIdFormatException Base64不正なIDが含まれる場合（「（Base64）」）
   */
  public List<StudentCourse> toEntityList(List<StudentCourseDto> dtoList, byte[] studentId) {
    return dtoList.stream()
        .map(dto -> new StudentCourse(
            Optional.ofNullable(dto.getCourseId())
                .filter(id -> !id.isBlank())
                .map(this::decodeBase64)
                .orElseGet(this::generateRandomBytes),
            studentId,
            dto.getCourseName(),
            dto.getStartDate(),
            dto.getEndDate(),
            null
        ))
        .collect(Collectors.toList());
  }

  /**
   * コースエンティティのリストをDTOリストに変換します。
   *
   * @param entities コースエンティティ一覧
   * @return コースDTO一覧
   * @throws IllegalArgumentException いずれかのIDが16バイト以外の場合
   */
  public List<StudentCourseDto> toDtoList(List<StudentCourse> entities) {
    return entities.stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  // ------------------------------------------------------------
  // StudentDetail（集約） 変換
  // ------------------------------------------------------------

  /**
   * 受講生とコースから詳細DTOを作成します。
   * <p>受講生ID・コースIDは Base64 文字列として詰められます。</p>
   *
   * @param student 受講生エンティティ
   * @param courses コースエンティティ一覧
   * @return 詳細DTO
   * @throws IllegalArgumentException IDが16バイト以外の場合
   */
  public StudentDetailDto toDetailDto(Student student, List<StudentCourse> courses) {
    StudentDto studentDto = toDto(student);
    List<StudentCourseDto> courseDtos = toDtoList(courses);
    return new StudentDetailDto(studentDto, courseDtos);
  }

  /**
   * {@link #toDetailDto(Student, List)} の拡張版。
   * <p>パスで受け取った Base64 ID（理論上DB返却と同一）を最終的に反映したい場合に使用します。</p>
   *
   * @param student          受講生エンティティ
   * @param courses          コースエンティティ一覧
   * @param base64IdOverride 上書きしたい Base64 学生ID
   * @return 詳細DTO（{@code base64IdOverride} が非nullなら学生IDを上書き）
   */
  @SuppressWarnings("unused")
  public StudentDetailDto toDetailDto(Student student, List<StudentCourse> courses,
      String base64IdOverride) {
    StudentDetailDto dto = toDetailDto(student, courses);
    if (dto != null && dto.getStudent() != null && base64IdOverride != null) {
      dto.getStudent().setStudentId(base64IdOverride);
    }
    return dto;
  }

  // ------------------------------------------------------------
  // ドメイン
  // ------------------------------------------------------------

  /**
   * 受講生とコースのエンティティからドメインモデルを生成します。
   *
   * @param student 受講生エンティティ
   * @param courses コースエンティティ一覧
   * @return ドメインモデル
   */
  @SuppressWarnings("unused")
  public StudentDetail toDomain(Student student, List<StudentCourse> courses) {
    return new StudentDetail(student, courses);
  }

  /**
   * 受講生／コースの一覧から詳細DTO一覧に変換します。
   * <p>コースは受講生ID（Base64）でグルーピングします。</p>
   *
   * @param students 受講生エンティティ一覧
   * @param courses  全コースエンティティ一覧
   * @return 詳細DTO一覧
   * @throws IllegalArgumentException いずれかのIDが16バイト以外の場合
   */
  public List<StudentDetailDto> toDetailDtoList(List<Student> students,
      List<StudentCourse> courses) {
    Map<String, List<StudentCourse>> courseMap = courses.stream()
        .collect(Collectors.groupingBy(course -> encodeBase64(course.getStudentId())));

    return students.stream()
        .map(student -> toDetailDto(student,
            courseMap.getOrDefault(encodeBase64(student.getStudentId()), List.of())))
        .collect(Collectors.toList());
  }

  // ------------------------------------------------------------
  // 部分更新マージ
  // ------------------------------------------------------------

  /**
   * 既存の受講生データに、新しいデータの null でないフィールドを上書きします（部分更新）。
   *
   * @param existing 現在の受講生エンティティ（更新対象）
   * @param update   部分更新用の受講生エンティティ（nullでないフィールドのみ採用）
   */
  public void mergeStudent(Student existing, Student update) {
    if (update.getFullName() != null) {
      existing.setFullName(update.getFullName());
    }
    if (update.getFurigana() != null) {
      existing.setFurigana(update.getFurigana());
    }
    if (update.getNickname() != null) {
      existing.setNickname(update.getNickname());
    }
    if (update.getEmail() != null) {
      existing.setEmail(update.getEmail());
    }
    if (update.getLocation() != null) {
      existing.setLocation(update.getLocation());
    }
    if (update.getAge() != null) {
      existing.setAge(update.getAge());
    }
    if (update.getGender() != null) {
      existing.setGender(update.getGender());
    }
    if (update.getRemarks() != null) {
      existing.setRemarks(update.getRemarks());
    }
  }
}


