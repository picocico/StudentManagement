package raisetech.student.management.controller.converter;

import java.util.Map;
import org.springframework.stereotype.Component;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.domain.StudentDetail;
import raisetech.student.management.dto.*;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 受講生・コースのエンティティ、DTO、ドメインモデル間の変換を行うコンバータークラス。
 */
@Component
public class StudentConverter {

  /**
   * StudentDto から Student エンティティに変換します。
   *
   * @param dto DTO形式の受講生情報
   * @return エンティティ形式の受講生情報
   */
  public Student toEntity(StudentDto dto) {
    return new Student(
        dto.getStudentId() != null ? dto.getStudentId() : UUID.randomUUID().toString(),
        dto.getFullName(),
        dto.getFurigana(),
        dto.getNickname(),
        dto.getEmail(),
        dto.getLocation(),
        dto.getAge(),
        dto.getGender(),
        dto.getRemarks(),
        null,
        null,
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
        entity.getStudentId(),
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
   * @param studentId 紐づく受講生ID
   * @return コースエンティティ
   */
  public StudentCourse toEntity(StudentCourseDto dto, String studentId) {
    return new StudentCourse(
        null,
        studentId,
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
        entity.getCourseName(),
        entity.getStartDate(),
        entity.getEndDate()
    );
  }

  /**
   * コースDTOのリストをエンティティリストに変換します。
   *
   * @param dtos DTOのリスト
   * @param studentId 紐づく受講生ID
   * @return エンティティリスト
   */
  public List<StudentCourse> toEntityList(List<StudentCourseDto> dtos, String studentId) {
    return dtos.stream()
        .map(dto -> toEntity(dto, studentId))
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
  public List<StudentDetailDto> convertStudentDetailsDto(List<Student> students, List<StudentCourse> courses) {
    Map<String, List<StudentCourse>> studentCourseMap = courses.stream()
        .collect(Collectors.groupingBy(StudentCourse::getStudentId));

    return students.stream()
        .map(student -> new StudentDetailDto(
            toDto(student),
            toDtoList(studentCourseMap.getOrDefault(student.getStudentId(), List.of()))
        ))
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


