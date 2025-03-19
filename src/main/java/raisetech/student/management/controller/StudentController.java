package raisetech.student.management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.domain.StudentDetail;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.service.StudentService;

@Controller
public class StudentController {

  private StudentService service;
  private StudentConverter converter;

  @Autowired
  public StudentController(StudentService service, StudentConverter converter) {
    this.service = service;
    this.converter = converter;
  }

  // ふりがな検索フォームの表示
  @GetMapping("/findStudent")
  public String findStudentPage() {
    return "findStudent";
  }

  // 生徒情報の編集画面を表示
  @GetMapping("/editStudent")
  public String editStudent(@RequestParam String studentId, Model model) {
    Student student = service.findStudentById(studentId);
    List<StudentCourse> studentCourses = service.searchCoursesByStudentId(studentId);

    StudentRegistrationRequest request = new StudentRegistrationRequest();
    request.setStudent(student);
    request.setCourses(studentCourses);

    model.addAttribute("studentRegistrationRequest", request);
    return "editStudent";
  }

  // 名前で受講生を検索
  @GetMapping("/findStudentsByFurigana")
  public String findStudentsByFurigana(@RequestParam String furigana, Model model) {
    List<Student> students = service.findStudentsByFurigana(furigana);
    model.addAttribute("students", students);
    return "studentFindResults";
  }

  // 生徒リストをサービスから取得　
  @GetMapping("/studentList")
  public String getStudentList(Model model) {
    List<Student> students = service.searchStudentList();
    List<StudentCourse> studentCourses = service.searchCourseList();

    model.addAttribute("studentList", converter.convertStudentDetails(students, studentCourses));
    return "studentList";
  }

  // 特定の生徒のコースだけをサービスから取得する API を追加
  @GetMapping("/courseListByStudentId")
  public List<StudentCourse> getCoursesByStudentId(@RequestParam String studentId) {
    return service.searchCoursesByStudentId(studentId);
  }

  // Thymeleaf でフォームのデータを StudentRegistrationRequest として受け取るように変更。
  @GetMapping("/newStudent")
  public String newStudent(Model model) {
    model.addAttribute("studentRegistrationRequest", new StudentRegistrationRequest());
    return "registerStudent";
  }

  @PostMapping("/registerStudent")
  public String registerStudent(@ModelAttribute StudentRegistrationRequest request,
      BindingResult result) {
    if (result.hasErrors()) {
      return "registerStudent";
    }
    service.registerStudentWithCourses(request);
    return "redirect:/studentList";
  }

  @PostMapping("/students")
  public ResponseEntity<StudentDetail> registerStudent(
      @RequestBody StudentRegistrationRequest request) {
    service.registerStudentWithCourses(request);
    return ResponseEntity.ok(new StudentDetail(request.getStudent(), request.getCourses()));
  }

  // 受講生情報の更新処理
  @PostMapping("/updateStudent")
  public String updateStudent(@ModelAttribute StudentRegistrationRequest request, BindingResult
      result) {
    if (result.hasErrors()) {
      return "editStudent";
    }
    service.updateStudentWithCourses(request);
    return "redirect:/studentList";
  }
}


