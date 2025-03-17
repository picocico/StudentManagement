package raisetech.student.management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.domain.StudentDetail;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.service.StudentService;

@Controller
@RequestMapping("/students")
public class StudentController {

  private StudentService service;
  private StudentConverter converter;

  @Autowired
  public StudentController(StudentService service, StudentConverter converter) {
    this.service = service;
    this.converter = converter;
  }
  // ServletとJSP

  /**【REST API】特定の生徒のコースだけJSONで取得 */
  @GetMapping("/courseListByStudentId")
  @ResponseBody
  public List<StudentCourse> getCoursesByStudentIdFromQuery(@RequestParam String studentId) {
    return service.searchCoursesByStudentId(studentId);
  }

  /**【REST API】特定の生徒情報をJSONで取得 */
  @GetMapping("/{studentId}")
  @ResponseBody // JSON レスポンスを返す
  public ResponseEntity<Student> getStudentByStudentId(@PathVariable String studentId) {
    Student student = service.findStudentByStudentId(studentId);
    return student != null ? ResponseEntity.ok(student) : ResponseEntity.notFound().build();
  }

  /** 【REST API】生徒の情報（コース情報も含む）をJSONで取得 */
  @GetMapping("/{studentId}/courses")
  @ResponseBody
  public ResponseEntity<List<StudentCourse>> getCoursesByStudentId(@PathVariable String studentId) {
    List<StudentCourse> courses = service.searchCoursesByStudentId(studentId);
    return courses != null ? ResponseEntity.ok(courses) : ResponseEntity.notFound().build();
  }

  /** 【REST API】生徒を登録（JSON 経由） */
  @PostMapping("/students")
  @ResponseBody
  public ResponseEntity<StudentDetail> registerStudentApi(
      @RequestBody StudentRegistrationRequest request) {
    service.registerStudentWithCourses(request);
    return ResponseEntity.ok(new StudentDetail(request.getStudent(), request.getCourses()));
  }

  /** 【Thymeleaf】フォームのデータを StudentRegistrationRequest として受け取るように変更。*/
  @GetMapping("/newStudent")
  public String newStudent(Model model) {
    model.addAttribute("studentRegistrationRequest", new StudentRegistrationRequest());
    return "registerStudent";
  }

  /** 【Thymeleaf】生徒を登録してリダイレクト */
  @PostMapping("/registerStudent")
  public String registerStudent(@ModelAttribute StudentRegistrationRequest request,
      BindingResult result) {
    if (result.hasErrors()) {
      return "registerStudent"; // エラーがある場合はフォームページを再表示
    }
    service.registerStudentWithCourses(request);
    return "redirect:/students";
  }
}

