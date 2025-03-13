package raisetech.student.management.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    return repository.search();
  }

  public List<StudentCourse> searchCourseList() {
    // 全てのコースリストを取得
    List<StudentCourse> courses = repository.findAllCourses();
    return repository.findAllCourses();
  }

  public List<StudentCourse> searchCoursesByStudentId(String studentId) {
    return repository.findCoursesByStudentId(studentId);
  }
}
