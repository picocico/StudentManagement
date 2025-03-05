package raisetech.student.management.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.repository.StudentRepository;

@Service
public class StudentService {

  private StudentRepository repository;

  @Autowired
  public StudentService(StudentRepository repository) {
    this.repository = repository;
  }

  public List<Student> searchStudentList() {
    // 生徒のリストを取得
    List<Student> students = repository.search();

    //　絞り込み。年齢が30代の人のみを抽出する。
    //　抽出したリストをコントローラーに返す。
    return students.stream()
        .filter(student -> student.getAge() >= 30)
        .collect(Collectors.toList());
  }

  public List<StudentCourse> searchCourseList() {
    // 全てのコースリストを取得
    List<StudentCourse> courses = repository.findAllCourses();

    // 絞り込み検索で「Javaコース」のコース情報のみを抽出する。
    // 抽出したリストをコントローラーに返す。
    return courses.stream()
        .filter(course -> "Javaコース".equals(course.getCourseName()))
        .collect(Collectors.toList());
  }

  public List<StudentCourse> searchCoursesByStudentId(@RequestParam String studentId) {
    return repository.findCoursesByStudentId(studentId);
  }
}
