package raisetech.student.management;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.repository.StudentRepository;

@SpringBootApplication
@RestController
public class Application {

  @Autowired
  public StudentRepository repository;

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  // 生徒リスト取得
  @GetMapping("/studentList")
  public List<Student> getStudentList() {
    return repository.search();
  }

  // 特定の生徒のコースだけを取得する API を追加
  @GetMapping("/courseListByStudentId")
  public List<StudentCourse> getCoursesByStudentId(@RequestParam String studentId) {
    return repository.findCoursesByStudentId(studentId);
  }

  // コースリスト取得
  @GetMapping("/courseList")
  public List<StudentCourse> getCourseList() {
    return repository.findAllCourses();
  }
}


