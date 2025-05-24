package raisetech.student.management.controller.converter;

import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.domain.StudentDetail;
import raisetech.student.management.dto.*;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import raisetech.student.management.util.UUIDUtil;

/**
 * 受講生・コースのエンティティ、DTO、ドメインモデル間の変換を行うコンバータークラス。
 */
@Component
public class StudentConverter {

  /**
   * URLセーフな Base64 文字列にエンコードします。
   * <p>
   * RFC 4648 に準拠し、エンコード結果にはpadding（末尾の "="）は含まれません。
   *
   * @param bytes エンコード対象のバイナリデータ（通常は UUID の byte[16]）
   * @return URLセーフな Base64 エンコード文字列（paddingなし）
   */
  public String encodeBase64(byte[] bytes) {
    return UUIDUtil.toBase64(bytes);
  }

  /**
   * URLセーフな Base64 文字列をデコードし、元のバイト配列に変換します。
   *
   * @param base64 Base64 文字列（パディングなし、URLセーフ形式）
   * @return デコードされたバイト配列（通常は UUID の byte[16]）
   */
  public byte[] decodeBase64(String base64) {
    return UUIDUtil.fromBase64(base64);
  }

  /**
   * ランダムなUUIDを生成し、それをバイト配列（16バイト）に変換して返します。
   * <p>
   * 生成されるバイト配列は、UUIDの128ビット（16バイト）のバイナリ表現です。
   *
   * @return ランダムなUUIDに基づく16バイトのバイト配列
   */
  public byte[] generateRandomBytes() {
    return UUIDUtil.fromUUID(UUID.randomUUID());
  }

  /**
   * StudentDto から Student エンティティに変換します。
   * <p>
   * 学生IDが指定されていない場合（null または 空文字）、新しいランダムUUIDを生成し、
   * それをバイナリ形式（byte[16]）に変換してセットします。
   *
   * @param dto DTO形式の受講生情報（Base64文字列形式の studentId を含む場合あり）
   * @return エンティティ形式の受講生情報（studentId は byte[] 型）
   */
  public Student toEntity(StudentDto dto) {
    // studentId が null または空文字の場合、新規 UUID を生成して byte[] に変換する。
    byte[] studentId = Optional.ofNullable(dto.getStudentId())
        .filter(id -> !id.isBlank())  // 空文字は無視
        .map(this::decodeBase64)
        .orElseGet(this::generateRandomBytes); // 新規 UUID を byte[] で生成

    // DTO の各フィールドを Student エンティティにマッピング
    return new Student(
        studentId,    // UUIDを16バイトのbyte[]としてセット
        dto.getFullName(),
        dto.getFurigana(),
        dto.getNickname(),
        dto.getEmail(),
        dto.getLocation(),
        dto.getAge(),
        dto.getGender(),
        dto.getRemarks(),
        null,              // createdAt はデータベース側で自動生成
        null,              // updatedAt も同様
        dto.getDeleted()
    );
  }

  /**
   * Student エンティティから StudentDto に変換します。
   *
   * @param entity エンティティ形式の受講生情報
   * @return DTO形式の受講生情報
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

  /**
   * StudentCourseDto から StudentCourse エンティティに変換します。
   *
   * @param dto コースDTO
   * @param studentIdBase64 紐づく受講生ID
   * @return コースエンティティ
   */
  public StudentCourse toEntity(StudentCourseDto dto, String studentIdBase64) {
    return new StudentCourse(
        null,
        decodeBase64(studentIdBase64),
        dto.getCourseName(),
        dto.getStartDate(),
        dto.getEndDate(),
        null
    );
  }

  /**
   * StudentCourse エンティティから DTO に変換します。
   *
   * @param entity コースエンティティ
   * @return コースDTO
   */
  public StudentCourseDto toDto(StudentCourse entity) {
    return new StudentCourseDto(
        encodeBase64(entity.getCourseId()),  // ← byte[] → String 変換
        entity.getCourseName(),
        entity.getStartDate(),
        entity.getEndDate()
    );
  }

  /**
   * コースDTOのリストをエンティティリストに変換します。
   *
   * @return エンティティリスト
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
   * @param entities エンティティリスト
   * @return DTOリスト
   */
  public List<StudentCourseDto> toDtoList(List<StudentCourse> entities) {
    return entities.stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  /**
   * 受講生とコースから詳細DTOを作成します。
   *
   * @param student 受講生情報
   * @param courses コース情報
   * @return 詳細DTO
   */
  public StudentDetailDto toDetailDto(Student student, List<StudentCourse> courses) {
    return new StudentDetailDto(toDto(student), toDtoList(courses));
  }

  /**
   * 受講生とコースをドメインモデルに変換します。
   *
   * @param student 受講生情報
   * @param courses コース情報
   * @return ドメインモデル
   */
  public StudentDetail toDomain(Student student, List<StudentCourse> courses) {
    return new StudentDetail(student, courses);
  }

  /**
   * 受講生とコースのリストから、詳細DTOのリストに変換します。
   *
   * @param students 受講生のリスト
   * @param courses 全受講生のコースのリスト
   * @return 詳細DTOのリスト
   */
  public List<StudentDetailDto> toDetailDtoList(List<Student> students, List<StudentCourse> courses) {
    Map<String, List<StudentCourse>> courseMap = courses.stream()
        .collect(Collectors.groupingBy(course -> encodeBase64(course.getStudentId())));

    return students.stream()
        .map(student -> toDetailDto(student, courseMap.getOrDefault(encodeBase64(student.getStudentId()), List.of())))
        .collect(Collectors.toList());
  }

  /**
   * 既存の受講生データに、新しいデータの null でないフィールドを上書きします（部分更新用）。
   *
   * @param existing 現在の受講生エンティティ
   * @param update 部分更新用のデータ
   */
  public void mergeStudent(Student existing, Student update) {
    if (update.getFullName() != null) existing.setFullName(update.getFullName());
    if (update.getFurigana() != null) existing.setFurigana(update.getFurigana());
    if (update.getNickname() != null) existing.setNickname(update.getNickname());
    if (update.getEmail() != null) existing.setEmail(update.getEmail());
    if (update.getLocation() != null) existing.setLocation(update.getLocation());
    if (update.getAge() != null) existing.setAge(update.getAge());
    if (update.getGender() != null) existing.setGender(update.getGender());
    if (update.getRemarks() != null) existing.setRemarks(update.getRemarks());
  }
}


