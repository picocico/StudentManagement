package raisetech.student.management.controller.converter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.domain.StudentDetail;
import raisetech.student.management.dto.StudentCourseDto;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentDto;
import raisetech.student.management.exception.InvalidIdFormatException;
import raisetech.student.management.util.IdCodec;

/**
 * 受講生・コースのエンティティ、DTO、ドメインモデル間の相互変換を担うコンバータ。
 *
 * <p>ID のエンコード／デコードは {@link IdCodec} に委譲し、
 * 本クラスでは {@link raisetech.student.management.exception.InvalidIdFormatException}
 * へのラップなどドメイン例外への変換のみを担当します。
 *
 * <h3>ID表現の方針</h3>
 *
 * <ul>
 *   <li>DBの主キーは {@code byte[16]}（UUIDの生バイト）を前提とします。
 *   <li>APIの入出力は URL-safe Base64（paddingなし）文字列を基本とします。
 *
 * <p>このクラスでは、文字列ID用のメソッド（{@link #decodeBase64ToBytes(String)} や
 * {@link #decodeIdOrThrow(String)}）では {@link InvalidIdFormatException} にラップして返し、
 * UUID/BINARY(16) 前提のメソッド（{@link #toEntity(StudentDto)} や
 * {@link #toEntityList(List, byte[])}）では {@link IdCodec} からの
 * {@link IllegalArgumentException} をそのまま伝播させます。
 */
@Component
@RequiredArgsConstructor
public class StudentConverter {
  // 既存のメソッド…（toDetailDto, toEntity など）

  private final IdCodec idCodec;

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
   * <p>長さチェック（16バイトかどうか）は {@link IdCodec#encodeId(byte[])} が責務を負います。
   * このメソッドでは null をそのまま null として返すことに専念します。
   *
   * @param bytes エンコード対象のバイナリ（通常は UUID の生16バイト）
   * @return URLセーフ Base64（paddingなし）の文字列。{@code bytes} が {@code null} の場合は {@code null}
   * @throws IllegalArgumentException {@code bytes} が16バイト以外の場合（IdCodec側で判定）
   */
  public String encodeBase64(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    // 長さチェックは IdCodec#encodeId に委譲する
    return idCodec.encodeId(bytes); // URL-safe & no padding 前提のユーティリティ
  }

  /**
   * URL-safe Base64 文字列をデコードしてバイト配列に変換します。
   *
   * <p>長さチェックは行いません（用途側で行います）。
   *
   * @param encoded URL-safe Base64（paddingなし）文字列
   * @return 復号後のバイト配列
   * @throws InvalidIdFormatException Base64 として不正な場合（「（Base64）」）
   */
  public byte[] decodeBase64ToBytes(String encoded) {
    try {
      return idCodec.decode(encoded);
    } catch (IllegalArgumentException e) {
      // IdCodec 側は IllegalArgumentException を投げる前提にしておいて、
      // ここで「ドメイン例外」にラップすることで、
      // 既存の InvalidIdFormatException を維持
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
   * ランダムなUUIDを生成し、その生16バイトを返します。
   *
   * @return ランダムUUIDの {@code byte[16]}
   */
  public byte[] generateRandomBytes() {
    return idCodec.generateNewIdBytes();
  }

  /**
   * URL-safe Base64 文字列をデコードし、UTF-8 文字列として扱う ID を復元します。
   *
   * <p>復元された文字列は {@code [0-9A-Za-z._-]} のみを許容し、それ以外の文字を含む場合は
   * {@link InvalidIdFormatException} をスローします。
   *
   * @param encoded URL-safe Base64 形式の文字列ID
   * @return 復元されたテキスト ID
   * @throws InvalidIdFormatException Base64 として不正、または許容されない文字を含む場合
   */
  public String decodeIdOrThrow(String encoded) {
    final byte[] bytes = decodeBase64ToBytes(encoded); // ← ここが IdCodec 経由になる

    final String id = new String(bytes, StandardCharsets.UTF_8);

    if (!ID_TEXT_PATTERN.matcher(id).matches()) {
      throw new InvalidIdFormatException("IDの形式が不正です（ID文字列）");
    }
    return id;
  }

  // ------------------------------------------------------------
  // Student 変換
  // ------------------------------------------------------------

  /**
   * {@link StudentDto} から {@link Student} エンティティに変換します。
   *
   * <p>{@code dto.studentId} が未指定（null または空文字）の場合は、新規にランダムUUIDを採番し、 その生16バイトをセットします。
   *
   * @param dto 受講生DTO（IDは Base64 文字列）
   * @return 受講生エンティティ（IDは生16バイト）
   * @throws InvalidIdFormatException Base64が不正な場合（「（Base64）」）
   */
  public Student toEntity(StudentDto dto) {
    byte[] studentId =
        Optional.ofNullable(dto.getStudentId())
            .filter(id -> !id.isBlank())
            .map(idCodec::decodeUuidBytesOrThrow)
            .orElseGet(idCodec::generateNewIdBytes);

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
        dto.getDeleted());
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
        entity.getDeleted());
  }

  // ------------------------------------------------------------
  // StudentCourse 変換
  // ------------------------------------------------------------

  /**
   * {@link StudentCourseDto} から {@link StudentCourse} エンティティに変換します。
   *
   * <p>{@code dto.courseId} が未指定なら新規採番し、{@code studentIdBase64} は必ず Base64 復号して紐付けます。
   *
   * @param dto             コースDTO（IDは Base64 文字列）
   * @param studentIdBase64 受講生ID（Base64 文字列）
   * @return コースエンティティ（IDは生16バイト）
   * @throws InvalidIdFormatException Base64が不正な場合（「（Base64）」）
   */
  @SuppressWarnings("unused")
  public StudentCourse toEntity(StudentCourseDto dto, String studentIdBase64) {
    byte[] courseId =
        Optional.ofNullable(dto.getCourseId())
            .filter(id -> !id.isBlank())
            .map(idCodec::decodeUuidBytesOrThrow)// ★ 16バイトチェック付き
            .orElseGet(idCodec::generateNewIdBytes);

    // studentId: パスから渡されるIDなので、必ず UUID 16バイトであることを保証する
    byte[] studentId = idCodec.decodeUuidBytesOrThrow(studentIdBase64);

    return new StudentCourse(
        courseId,
        studentId,
        dto.getCourseName(),
        dto.getStartDate(),
        dto.getEndDate(),
        null // createdAt（DB側）
    );
  }

  /**
   * コースDTOのリストをエンティティリストに変換します。
   *
   * <p>各 {@code dto.courseId} が未指定なら新規採番し、{@code studentId} を紐付けます。
   *
   * @param dtoList   コースDTO一覧
   * @param studentId 紐付け先の受講生ID（生16バイト）
   * @return コースエンティティ一覧
   * @throws InvalidIdFormatException Base64不正なIDが含まれる場合（「（Base64）」）
   */
  public List<StudentCourse> toEntityList(List<StudentCourseDto> dtoList, byte[] studentId) {
    return dtoList.stream()
        .map(
            dto -> {
              byte[] courseId =
                  Optional.ofNullable(dto.getCourseId())
                      .filter(id -> !id.isBlank())
                      .map(idCodec::decodeUuidBytesOrThrow)
                      .orElseGet(idCodec::generateNewIdBytes);
              return new StudentCourse(
                  courseId,
                  studentId,
                  dto.getCourseName(),
                  dto.getStartDate(),
                  dto.getEndDate(),
                  null);
            })
        .collect(Collectors.toList());
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
        entity.getEndDate());
  }

  /**
   * コースエンティティのリストをDTOリストに変換します。
   *
   * @param entities コースエンティティ一覧
   * @return コースDTO一覧
   * @throws IllegalArgumentException いずれかのIDが16バイト以外の場合
   */
  public List<StudentCourseDto> toDtoList(List<StudentCourse> entities) {
    return entities.stream().map(this::toDto).collect(Collectors.toList());
  }

  // ------------------------------------------------------------
  // StudentDetail（集約） 変換
  // ------------------------------------------------------------

  /**
   * 受講生とコースから詳細DTOを作成します。
   *
   * <p>受講生ID・コースIDは Base64 文字列として詰められます。
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
   *
   * <p>パスで受け取った Base64 ID（理論上DB返却と同一）を最終的に反映したい場合に使用します。
   *
   * @param student          受講生エンティティ
   * @param courses          コースエンティティ一覧
   * @param base64IdOverride 上書きしたい Base64 学生ID
   * @return 詳細DTO（{@code base64IdOverride} が非nullなら学生IDを上書き）
   */
  @SuppressWarnings("unused")
  public StudentDetailDto toDetailDto(
      Student student, List<StudentCourse> courses, String base64IdOverride) {
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
   *
   * <p>コースは受講生ID（Base64）でグルーピングします。
   *
   * @param students 受講生エンティティ一覧
   * @param courses  全コースエンティティ一覧
   * @return 詳細DTO一覧
   * @throws IllegalArgumentException いずれかのIDが16バイト以外の場合
   */
  public List<StudentDetailDto> toDetailDtoList(
      List<Student> students, List<StudentCourse> courses) {
    Map<String, List<StudentCourse>> courseMap =
        courses.stream()
            .collect(Collectors.groupingBy(course -> encodeBase64(course.getStudentId())));

    return students.stream()
        .map(
            student ->
                toDetailDto(
                    student,
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
