package raisetech.student.management.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.service.StudentService;


/**
 * 受講生に関するREST APIを提供するコントローラークラス。
 * <p>
 * このクラスは、受講生の登録、取得、更新、削除、復元、およびふりがなによる検索などの
 * 操作をエンドポイントとして提供します。
 */
@RestController
@RequestMapping("/api/students")
@Validated
@RequiredArgsConstructor
public class StudentController {

  /** ロガー */
  private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

  /** 受講生サービス */
  private final StudentService service;

  /** 受講生コンバーター */
  private final StudentConverter converter;

  /**
   * 新規の受講生情報を登録します。
   *
   * @param request 登録する受講生情報およびコース情報
   * @return 201 Created
   */
  @PostMapping
  public ResponseEntity<Void> registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
    Student student = converter.toEntity(request.getStudent());
    List<StudentCourse> courses = converter.toEntityList(request.getCourses(), student.getStudentId());

    logger.debug("POST - Registering new student: {}", student.getFullName());
    service.registerStudent(student, courses);
    return ResponseEntity.status(201).build();
  }

  /**
   * 条件付きで受講生一覧を取得します（ふりがな、削除状態）。
   *
   * @param furigana        ふりがな検索（省略可能）
   * @param includeDeleted  論理削除済みも含めるか
   * @param deletedOnly     論理削除された学生のみ取得するか
   * @return 条件に一致する受講生詳細DTOリスト
   */
  @GetMapping
  public ResponseEntity<List<StudentDetailDto>> getStudentList(
      @RequestParam(required = false) String furigana,
      @RequestParam(required = false, defaultValue = "false") boolean includeDeleted,
      @RequestParam(required = false, defaultValue = "false") boolean deletedOnly) {

    logger.debug("GET - Fetching students list. furigana={}, includeDeleted={}, deletedOnly={}",
        furigana, includeDeleted, deletedOnly);

    List<StudentDetailDto> students = service.getStudentList(furigana, includeDeleted, deletedOnly);

    return ResponseEntity.ok(students);
  }

  /**
   * 指定された受講生IDに該当する詳細情報を取得します。
   *
   * @param studentId 受講生ID
   * @return 受講生詳細情報
   */
  @GetMapping("/{studentId}")
  public ResponseEntity<StudentDetailDto> getStudentDetail(@PathVariable String studentId) {
    logger.debug("GET - Fetching student detail: {}", studentId);
    Student student = service.findStudentById(studentId);
    List<StudentCourse> courses = service.searchCoursesByStudentId(studentId);
    return ResponseEntity.ok(converter.toDetailDto(student, courses));
  }

  /**
   * 受講生情報を全体的に更新します。
   *
   * @param studentId 受講生ID
   * @param request 更新内容
   * @return 更新後の受講生詳細情報
   */
  @PutMapping("/{studentId}")
  public ResponseEntity<StudentDetailDto> updateStudent(
      @PathVariable String studentId,
      @Valid @RequestBody StudentRegistrationRequest request) {

    Student student = converter.toEntity(request.getStudent());
    student.setStudentId(studentId);
    student.setDeleted(request.isDeleted());
    List<StudentCourse> courses = converter.toEntityList(request.getCourses(), studentId);

    logger.debug("PUT - Updating student: {}", studentId);

    service.updateStudent(student, courses);
    Student updated = service.findStudentById(studentId);
    List<StudentCourse> updatedCourses = service.searchCoursesByStudentId(studentId);
    return ResponseEntity.ok(converter.toDetailDto(updated, updatedCourses));
  }

  /**
   * 受講生情報を部分的に更新します。
   *
   * @param studentId 受講生ID
   * @param request 更新対象のフィールド
   * @return 更新後の受講生詳細情報
   */
  @PatchMapping("/{studentId}")
  public ResponseEntity<StudentDetailDto> partialUpdateStudent(
      @PathVariable String studentId,
      @RequestBody StudentRegistrationRequest request) {

    Student existing = service.findStudentById(studentId);
    Student update = converter.toEntity(request.getStudent());
    converter.mergeStudent(existing, update);
    List<StudentCourse> convertedCourses = converter.toEntityList(request.getCourses(), studentId);

    logger.debug("PATCH - Partially updating student: {}", studentId);

    service.partialUpdateStudent(existing, convertedCourses);
    Student updated = service.findStudentById(studentId);
    List<StudentCourse> updatedCourses = service.searchCoursesByStudentId(studentId);
    return ResponseEntity.ok(converter.toDetailDto(updated, updatedCourses));
  }

  /**
   * 受講生情報を論理削除します。
   *
   * @param studentId 削除対象の受講生ID
   * @return 204 No Content
   */
  @DeleteMapping("/{studentId}")
  public ResponseEntity<Void> deleteStudent(@PathVariable String studentId) {
    logger.debug("DELETE - Logically deleting student: {}", studentId);
    service.softDeleteStudent(studentId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 論理削除された受講生情報を復元します。
   *
   * @param studentId 復元対象の受講生ID
   * @return 204 No Content
   */
  @PatchMapping("/{studentId}/restore")
  public ResponseEntity<Void> restoreStudent(@PathVariable String studentId) {
    logger.debug("PATCH - Restoring student: {}", studentId);
    service.restoreStudent(studentId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 指定されたIDの受講生情報を物理削除します。
   *
   * @param studentId 削除対象の受講生ID
   * @return 削除成功時は204 No Content
   */
  @DeleteMapping("/{studentId}/force")
  public ResponseEntity<Void> deleteStudentPhysically(@PathVariable String studentId) {
    service.deleteStudentPhysically(studentId);
    return ResponseEntity.noContent().build();
  }
}


