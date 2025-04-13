package raisetech.student.management.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
import raisetech.student.management.domain.StudentDetail;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.service.StudentService;

@RequestMapping("/api/students")
@Validated
@RestController
public class StudentController {

  private  static final Logger logger = LoggerFactory.getLogger(StudentController.class);
  private final StudentService service;
  private final StudentConverter converter;

  @Autowired
  public StudentController(StudentService service, StudentConverter converter) {
    this.service = service;
    this.converter = converter;
  }

  // 受講生一覧（論理削除されていないもの）
  @GetMapping
  public ResponseEntity<List<StudentDetail>> getStudentList() {
    List<StudentDetail> students = service.searchActiveStudentList();
    List<StudentCourse> studentCourses = service.searchCourseList();
    return ResponseEntity.ok(service.searchActiveStudentList());
  }

  // 受講生一覧（論理削除されたものも含む）
  @GetMapping("/all")
  public ResponseEntity<List<StudentDetail>> getAllStudentsIncludingDeleted() {
    List<StudentDetail> students = service.searchStudentList(); // ← ここで全件取得
    List<StudentCourse> studentCourses = service.searchCourseList();
    return ResponseEntity.ok(service.searchStudentList());

  }

  // ふりがな検索
  @GetMapping("/search")
  public ResponseEntity<List<Student>> findStudentsByFurigana(@RequestParam String furigana) {
    return ResponseEntity.ok(service.findStudentsByFurigana(furigana));
  }

  // 単一受講生の詳細取得
  @GetMapping("/{studentId}")
  public ResponseEntity<StudentRegistrationRequest> getStudentById(@PathVariable String studentId) {
    Student student = service.findStudentById(studentId);
    List<StudentCourse> courses = service.searchCoursesByStudentId(studentId);
    StudentRegistrationRequest request = new StudentRegistrationRequest(student, courses, student.getDeleted());
    return ResponseEntity.ok(request);
  }

  // 特定の受講生のコース取得
  @GetMapping("/{studentId}/courses")
  public ResponseEntity<List<StudentCourse>> getCoursesByStudentId(@PathVariable String studentId) {
    return ResponseEntity.ok(service.searchCoursesByStudentId(studentId));
  }

  // 受講生の新規登録
  @PostMapping
  public ResponseEntity<StudentDetail> registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
    service.registerStudentWithCourses(request);
    return ResponseEntity.ok(new StudentDetail(request.getStudent(), request.getCourses()));
  }

  // 受講生情報の更新
  @PutMapping("/{studentId}")
  public ResponseEntity<List<StudentDetail>> updateStudent(@PathVariable String studentId,
      @Valid @RequestBody StudentRegistrationRequest request) {
    logger.debug("PUT - Updating full student: {}", studentId);
    request.getStudent().setStudentId(studentId); // パスパラメータで渡したIDを強制セット
    service.updateStudentWithCourses(request); // 更新処理
    List<StudentDetail> students = service.searchStudentList(); // 更新後に最新一覧（論理削除含む）を取得して返却
    return ResponseEntity.ok(students);
  }

  // 部分更新（PATCH）
  @PatchMapping("/{studentId}")
  public ResponseEntity<StudentDetail> partialUpdateStudent(
      @PathVariable String studentId,
      @RequestBody StudentRegistrationRequest request) {

    logger.debug("PATCH - Partially updating student: {}", studentId);
    request.getStudent().setStudentId(studentId);

    service.partialUpdateStudentWithCourses(request);

    // ✅ 更新後の student を再取得して返す！
    Student updatedStudent = service.findStudentById(studentId);
    List<StudentCourse> updatedCourses = service.searchCoursesByStudentId(studentId);

    StudentDetail detail = new StudentDetail(updatedStudent, updatedCourses);
    return ResponseEntity.ok(detail);
  }

  // 受講生情報の完全削除
  @DeleteMapping("/{studentId}")
  public ResponseEntity<String> deleteStudent(@PathVariable String studentId) {
    service.deleteStudent(studentId);
    return ResponseEntity.ok("受講生情報を削除しました");
  }
}
