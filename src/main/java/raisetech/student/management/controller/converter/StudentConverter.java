package raisetech.student.management.controller.converter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.domain.ApplicationStatus;
import raisetech.student.management.domain.StudentDetail;
import raisetech.student.management.dto.StudentCourseDto;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentDto;
import raisetech.student.management.exception.InvalidIdFormatException;
import raisetech.student.management.util.IdCodec;

/**
 * 受講生・コースのエンティティ、DTO、ドメインモデル間の相互変換を担うコンバータ。
 *
 * <p>ID のエンコード／デコードは {@link IdCodec} に委譲し、 本クラスでは {@link
 * raisetech.student.management.exception.InvalidIdFormatException} へのラップなどドメイン例外への変換のみを担当します。
 *
 * <h3>ID表現の方針</h3>
 *
 * <ul>
 *   <li>DBの主キーは UUID を BINARY(16) 型で保持します（マッピングは TypeHandler が担当）。
 *   <li>APIの入出力は、標準的な UUID 文字列表現 （例: {@code 123e4567-e89b-12d3-a456-426614174000}）を使用します。
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

  /** 新規UUIDを発番します。 */
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
   * <p>{@code dto.courseId} が未指定なら新規採番し、 {@code studentId} は UUID 文字列としてデコードして紐付けます。
   *
   * @param dto コースDTO
   * @param studentId 受講生ID（UUID 文字列）
   */
  @SuppressWarnings("unused")
  public StudentCourse toEntity(StudentCourseDto dto, String studentId) {
    UUID courseId =
        Optional.ofNullable(dto.getCourseId())
            .filter(id -> !id.isBlank())
            .map(this::decodeUuidStringOrThrow) // ラッパー経由で必ずInvalidIdFormatExceptionが飛ぶ
            .orElseGet(this::generateRandomUuid);

    // studentId: パスから渡されるIDなので、必ず UUID 16バイトであることを保証する
    UUID studentIdBytes = decodeUuidStringOrThrow(studentId); // ★ ここもラッパー経由

    StudentCourse course = new StudentCourse();
    course.setCourseId(courseId);
    course.setStudentId(studentIdBytes);
    course.setCourseName(dto.getCourseName());
    course.setStartDate(dto.getStartDate());
    course.setEndDate(dto.getEndDate());
    course.setApplicationStatus(null); // DBのJOINで取得するのでここでは未設定
    course.setCreatedAt(null); // DB側で生成
    return course;
  }

  /**
   * コースDTOのリストをエンティティリストに変換します。
   *
   * @param dtoList コースDTO一覧
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
              StudentCourse course = new StudentCourse();
              course.setCourseId(courseId);
              course.setStudentId(studentId);
              course.setCourseName(dto.getCourseName());
              course.setStartDate(dto.getStartDate());
              course.setEndDate(dto.getEndDate());
              course.setApplicationStatus(null); // JOINで埋まる
              course.setCreatedAt(null); // DB側
              return course;
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
    String code = entity.getApplicationStatus(); // "IN_PROGRESS" 等（nullもあり得る）
    String label = ApplicationStatus.fromCode(code).map(ApplicationStatus::getLabel).orElse(null);

    return new StudentCourseDto(
        encodeUuidString(entity.getCourseId()),
        entity.getCourseName(),
        entity.getStartDate(),
        entity.getEndDate(),
        code,
        label);
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

  /**
   * PATCH/PUT 用：courses DTO を StudentCourse エンティティへ変換します。
   *
   * <p>courseId が未指定（null/blank）の場合は null のままにし、 Service側で「追加(insert)」として扱えるようにします。
   *
   * @param studentId パスで指定された受講生ID
   * @param dtoList コースDTO一覧
   * @return コースエンティティ一覧
   */
  public List<StudentCourse> toCourseEntities(UUID studentId, List<StudentCourseDto> dtoList) {
    if (studentId == null) {
      throw new InvalidIdFormatException("IDはnullにできません（UUID）");
    }
    if (dtoList == null) {
      throw new IllegalArgumentException("courses must not be null");
    }

    return dtoList.stream()
        .map(
            dto -> {
              StudentCourse c = new StudentCourse();

              // ★ここが既存の toEntityList と違う：未指定なら採番しない
              UUID courseId =
                  Optional.ofNullable(dto.getCourseId())
                      .filter(id -> !id.isBlank())
                      .map(this::decodeUuidStringOrThrow)
                      .orElse(null);
              c.setCourseId(courseId);

              c.setStudentId(studentId);
              c.setCourseName(dto.getCourseName());
              c.setStartDate(dto.getStartDate());
              c.setEndDate(dto.getEndDate());

              // ★PATCHでstatus更新したいので詰める（空白はnull扱い）
              c.setApplicationStatus(blankToNull(dto.getApplicationStatus()));

              c.setCreatedAt(null);
              return c;
            })
        .toList();
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
   * @param student 受講生エンティティ
   * @param courses コースエンティティ一覧
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
   * @param courses 全コースエンティティ一覧
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
   * @param update 部分更新用の受講生エンティティ（nullでないフィールドのみ採用）
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

  /**
   * PATCH 用：StudentDto から「更新差分」だけを持つ Student エンティティを生成します。
   *
   * <p>studentId は必ず引数のものを使用し、DTO内の studentId は無視します。
   *
   * <p>空文字/空白のみは “未指定” とみなし null に正規化します。
   *
   * @param studentId パスで指定された受講生ID
   * @param dto 部分更新用DTO
   * @return 更新差分を持つ Student
   */
  public Student toStudentEntityForPatch(UUID studentId, StudentDto dto) {
    if (studentId == null) {
      throw new InvalidIdFormatException("IDはnullにできません（UUID）");
    }
    if (dto == null) {
      throw new IllegalArgumentException("student dto must not be null");
    }

    Student s = new Student();
    s.setStudentId(studentId);

    s.setFullName(blankToNull(dto.getFullName()));
    s.setFurigana(blankToNull(dto.getFurigana()));
    s.setNickname(blankToNull(dto.getNickname()));
    s.setEmail(blankToNull(dto.getEmail()));
    s.setLocation(blankToNull(dto.getLocation()));
    s.setAge(dto.getAge());
    s.setGender(blankToNull(dto.getGender()));
    s.setRemarks(blankToNull(dto.getRemarks()));

    // PATCHでdeletedをstudent側で扱う設計じゃないため触らない（nullのまま）
    // s.setDeleted(dto.getDeleted());

    return s;
  }

  private String blankToNull(String v) {
    return (v == null || v.isBlank()) ? null : v;
  }
}
