package raisetech.student.management.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.service.StudentService;

@Controller
@RequestMapping("/students")
public class StudentViewController {

  private final StudentService service;
  private final StudentConverter converter;

  @Autowired
  public StudentViewController(StudentService service, StudentConverter converter) {
    this.service = service;
    this.converter = converter;
  }

  // 生徒リストをサービスから取得
  @GetMapping("/studentList")
  public String getStudentList(Model model) {
    List<Student> students = service.searchStudentList();
    List<StudentCourse> studentCourses = service.searchCourseList();
    model.addAttribute("studentList", converter.convertStudentDetails(students, studentCourses));
    return "studentList";
  }
}

