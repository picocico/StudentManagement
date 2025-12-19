package raisetech.student.management.controller.converter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
 *   <li>DBの主キーは UUID を BINARY(16) 型で保持します（マッピングは TypeHandler が担当）。</li>
 *   <li>APIの入出力は、標準的な UUID 文字列表現
 *       （例: {@code 123e4567-e89b-12d3-a456-426614174000}）を使用します。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class StudentConverter {
  // 既存のメソッド…（toDetailDto, toEntity など）

  // ------------------------------------------------------------
  // UUID 文字列表現 ⇔ UUID の変換ヘルパー
  // ------------------------------------------------------------

  /**
   * UUID を標準的な文字列表現に変換します。
   *
   * @param uuid 変換対象のUUID（null不可）
   * @return 標準形式のUUID文字列
   * @throws InvalidIdFormatException uuid が null の場合
   */
  public String encodeUuidString(UUID uuid) {
    if (uuid == null) {
      throw new InvalidIdFormatException("IDはnullにできません（UUID）");
    }
    return uuid.toString();
  }

  /**
   * UUID文字列表現をUUIDに変換します。
   *
   * @param uuidString UUID文字列表現
   * @return UUIDオブジェクト
   * @throws InvalidIdFormatException null / 空文字 / 形式不正の場合
   */
  public UUID decodeUuidStringOrThrow(String uuidString) {
    if (uuidString == null || uuidString.isBlank()) {
      throw new InvalidIdFormatException("IDは必須です（UUID文字列）");
    }
    try {
      return UUID.fromString(uuidString);
    } catch (IllegalArgumentException e) {
      throw new InvalidIdFormatException("IDの形式が不正です（UUID）", e);
    }
  }

  /**
   * 新規UUIDを発番します。
   */
  public UUID generateRandomUuid() {
    return UUID.randomUUID();
  }

  // ------------------------------------------------------------
  // Student 変換
  // ------------------------------------------------------------

  /**
   * {@link StudentDto} から {@link Student} エンティティに変換します。
   *
   * <p>{@code dto.studentId} が未指定（null / 空文字）の場合は、新規に UUID を採番します。
   */
  public Student toEntity(StudentDto dto) {
    UUID studentId =
        Optional.ofNullable(dto.getStudentId())
            .filter(id -> !id.isBlank())
            .map(this::decodeUuidStringOrThrow)
            .orElseGet(this::generateRandomUuid);

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
   * <p>受講生IDは UUID文字列表現としてDTOに詰めます。
   */
  public StudentDto toDto(Student entity) {
    return new StudentDto(
        encodeUuidString(entity.getStudentId()),
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
   * <p>{@code dto.courseId} が未指定なら新規採番し、
   * {@code studentId} は UUID 文字列としてデコードして紐付けます。
   *
   * @param dto       コースDTO
   * @param studentId 受講生ID（UUID 文字列）
   */
  @SuppressWarnings("unused")
  public StudentCourse toEntity(StudentCourseDto dto, String studentId) {
    UUID courseId =
        Optional.ofNullable(dto.getCourseId())
            .filter(id -> !id.isBlank())
            .map(this::decodeUuidStringOrThrow) //ラッパー経由で必ずInvalidIdFormatExceptionが飛ぶ
            .orElseGet(this::generateRandomUuid);

    // studentId: パスから渡されるIDなので、必ず UUID 16バイトであることを保証する
    UUID studentIdBytes = decodeUuidStringOrThrow(studentId); // ★ ここもラッパー経由

    return new StudentCourse(
        courseId,
        studentIdBytes,
        dto.getCourseName(),
        dto.getStartDate(),
        dto.getEndDate(),
        null // createdAt（DB側）
    );
  }

  /**
   * コースDTOのリストをエンティティリストに変換します。
   *
   * @param dtoList   コースDTO一覧
   * @param studentId 紐付け先の受講生ID（UUID）
   * @return コースエンティティ一覧
   */
  public List<StudentCourse> toEntityList(List<StudentCourseDto> dtoList, UUID studentId) {
    return dtoList.stream()
        .map(
            dto -> {
              UUID courseId =
                  Optional.ofNullable(dto.getCourseId())
                      .filter(id -> !id.isBlank())
                      .map(this::decodeUuidStringOrThrow)
                      .orElseGet(this::generateRandomUuid);
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
   * @return コースDTO（IDは UUID 文字列）
   * @throws InvalidIdFormatException エンティティに保持されている ID のバイト長が 16 バイト以外など、 ID の形式が不正な場合
   */
  public StudentCourseDto toDto(StudentCourse entity) {
    return new StudentCourseDto(
        encodeUuidString(entity.getCourseId()),
        entity.getCourseName(),
        entity.getStartDate(),
        entity.getEndDate());
  }

  /**
   * コースエンティティのリストをDTOリストに変換します。
   *
   * @param entities コースエンティティ一覧
   * @return コースDTO一覧
   * @throws InvalidIdFormatException いずれかの ID の形式が不正な場合
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
   * <p>受講生ID・コースIDは UUID 文字列表現として詰められます。
   *
   * @param student 受講生エンティティ
   * @param courses コースエンティティ一覧
   * @return 詳細DTO
   * @throws InvalidIdFormatException ID の形式が不正な場合
   */
  public StudentDetailDto toDetailDto(Student student, List<StudentCourse> courses) {
    StudentDto studentDto = toDto(student);
    List<StudentCourseDto> courseDtos = toDtoList(courses);
    return new StudentDetailDto(studentDto, courseDtos);
  }

  /**
   * {@link #toDetailDto(Student, List)} の拡張版。
   *
   * <p>パスで受け取った UUID 文字列（理論上DB返却と同一）を最終的に反映したい場合に使用します。
   *
   * @param student           受講生エンティティ
   * @param courses           コースエンティティ一覧
   * @param studentIdOverride 上書きしたい学生ID（UUID文字列）
   * @return 詳細DTO（{@code studentIdOverride} が非nullなら学生IDを上書き）
   */
  @SuppressWarnings("unused")
  public StudentDetailDto toDetailDto(
      Student student, List<StudentCourse> courses, String studentIdOverride) {
    StudentDetailDto dto = toDetailDto(student, courses);
    if (dto != null && dto.getStudent() != null && studentIdOverride != null) {
      dto.getStudent().setStudentId(studentIdOverride);
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
   * <p>コースは受講生ID（UUID）でグルーピングします。
   *
   * @param students 受講生エンティティ一覧
   * @param courses  全コースエンティティ一覧
   * @return 詳細DTO一覧
   * @throws InvalidIdFormatException ID の形式が不正な場合
   */
  public List<StudentDetailDto> toDetailDtoList(
      List<Student> students, List<StudentCourse> courses) {
    Map<String, List<StudentCourse>> courseMap =
        courses.stream()
            .collect(Collectors.groupingBy(course -> encodeUuidString(course.getStudentId())));

    return students.stream()
        .map(
            student ->
                toDetailDto(
                    student,
                    courseMap.getOrDefault(encodeUuidString(student.getStudentId()), List.of())))
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
