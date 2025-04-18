package raisetech.student.management.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.*;
import raisetech.student.management.service.StudentService;

import java.util.List;

/**
 * 学生に関するREST APIを提供するコントローラークラス。
 * <p>
 * このクラスは、学生の登録、取得、更新、削除、復元、およびふりがなによる検索などの
 * 操作をエンドポイントとして提供します。
 */
@RestController
@RequestMapping("/api/students")
@Validated
@RequiredArgsConstructor
public class StudentController {

  /** ロガー */
  private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

  /** 学生サービス */
  private final StudentService service;

  /** 学生コンバーター */
  private final StudentConverter converter;

  /**
   * 新規の学生情報を登録します。
   *
   * @param request 登録する学生情報およびコース情報
   * @return レスポンスステータス
   */
  @PostMapping
  public ResponseEntity<Void> registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
    logger.debug("POST - Registering new student");
    Student student = converter.toEntity(request.getStudent());
    List<StudentCourse> courses = converter.toEntityList(request.getCourses(), student.getStudentId());
    service.registerStudent(student, courses);
    return ResponseEntity.ok().build();
  }

  /**
   * 論理削除されていない学生の一覧を取得します。
   *
   * @return 学生詳細情報リスト
   */
  @GetMapping
  public ResponseEntity<List<StudentDetailDto>> getStudentList() {
    logger.debug("GET - Fetching all active students");
    List<Student> students = service.searchActiveStudents();
    List<StudentCourse> courses = service.searchAllCourses();
    List<StudentDetailDto> response = converter.convertStudentDetailsDto(students, courses);
    return ResponseEntity.ok(response);
  }

  /**
   * 論理削除済みも含む、すべての学生の一覧を取得します。
   *
   * @return 学生詳細情報のリスト
   */
  @GetMapping("/all")
  public ResponseEntity<List<StudentDetailDto>> getAllStudentsIncludingDeleted() {
    logger.debug("GET - Fetching all students including deleted");
    List<Student> students = service.searchAllStudents(); // is_deleted 見ない
    List<StudentCourse> courses = service.searchAllCourses();
    List<StudentDetailDto> response = converter.convertStudentDetailsDto(students, courses);
    return ResponseEntity.ok(response);
  }

  /**
   * 指定された学生IDに該当する詳細情報を取得します。
   *
   * @param studentId 学生ID
   * @return 学生詳細情報
   */
  @GetMapping("/{studentId}")
  public ResponseEntity<StudentDetailDto> getStudentDetail(@PathVariable String studentId) {
    logger.debug("GET - Fetching student detail: {}", studentId);
    Student student = service.findStudentById(studentId);
    List<StudentCourse> courses = service.searchCoursesByStudentId(studentId);
    return ResponseEntity.ok(converter.toDetailDto(student, courses));
  }

  /**
   * 学生情報を全体的に更新します。
   *
   * @param studentId 学生ID
   * @param request 更新内容
   * @return 更新後の学生詳細情報
   */
  @PutMapping("/{studentId}")
  public ResponseEntity<StudentDetailDto> updateStudent(
      @PathVariable String studentId,
      @Valid @RequestBody StudentRegistrationRequest request) {
    logger.debug("PUT - Updating student: {}", studentId);
    Student student = converter.toEntity(request.getStudent());
    student.setStudentId(studentId);
    student.setDeleted(request.isDeleted());
    List<StudentCourse> courses = converter.toEntityList(request.getCourses(), studentId);
    service.updateStudent(student, courses);
    Student updated = service.findStudentById(studentId);
    List<StudentCourse> updatedCourses = service.searchCoursesByStudentId(studentId);
    return ResponseEntity.ok(converter.toDetailDto(updated, updatedCourses));
  }

  /**
   * 学生情報を部分的に更新します。
   *
   * @param studentId 学生ID
   * @param request 更新対象のフィールド
   * @return 更新後の学生詳細情報
   */
  @PatchMapping("/{studentId}")
  public ResponseEntity<StudentDetailDto> partialUpdateStudent(
      @PathVariable String studentId,
      @RequestBody StudentRegistrationRequest request) {
    logger.debug("PATCH - Partially updating student: {}", studentId);
    Student existing = service.findStudentById(studentId);
    Student update = converter.toEntity(request.getStudent());
    converter.mergeStudent(existing, update);

    List<StudentCourse> convertedCourses = converter.toEntityList(request.getCourses(), studentId);
    service.partialUpdateStudent(existing, convertedCourses);

    Student updated = service.findStudentById(studentId);
    List<StudentCourse> updatedCourses = service.searchCoursesByStudentId(studentId);
    return ResponseEntity.ok(converter.toDetailDto(updated, updatedCourses));
  }

  /**
   * 指定されたふりがなで学生を検索します。
   *
   * @param furigana 検索キーワード
   * @return 該当する学生詳細情報のリスト
   */
  @GetMapping("/search")
  public ResponseEntity<List<StudentDetailDto>> searchByFurigana(@RequestParam String furigana) {
    logger.debug("GET - Searching students by furigana: {}", furigana);
    List<Student> students = service.findStudentsByFurigana(furigana);
    List<StudentCourse> courses = service.searchAllCourses();
    List<StudentDetailDto> response = converter.convertStudentDetailsDto(students, courses);
    return ResponseEntity.ok(response);
  }

  /**
   * 学生情報を論理削除します。
   *
   * @param studentId 削除対象の学生ID
   * @return 204 No Content
   */
  @DeleteMapping("/{studentId}")
  public ResponseEntity<Void> deleteStudent(@PathVariable String studentId) {
    logger.debug("DELETE - Logically deleting student: {}", studentId);
    service.softDeleteStudent(studentId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 論理削除された学生情報を復元します。
   *
   * @param studentId 復元対象の学生ID
   * @return 204 No Content
   */
  @PatchMapping("/{studentId}/restore")
  public ResponseEntity<Void> restoreStudent(@PathVariable String studentId) {
    logger.debug("PATCH - Restoring student: {}", studentId);
    service.restoreStudent(studentId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 指定されたIDの学生情報を物理削除します。
   *
   * @param studentId 削除対象の学生ID
   * @return 削除成功時は204 No Content
   */
  @DeleteMapping("/{studentId}/force")
  public ResponseEntity<Void> deleteStudentPhysically(@PathVariable String studentId) {
    service.deleteStudentPhysically(studentId);
    return ResponseEntity.noContent().build();
  }
}


