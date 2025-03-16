package raisetech.student.management.service;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentRegistrationRequest;
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

  // @Transactional をつけることで、処理がすべて成功しないとデータが保存されないようにする。
  @Transactional
  public void registerStudentWithCourses(StudentRegistrationRequest request) {
    // 学生情報の登録
    repository.insertStudent(request.getStudent());

    // コース情報の登録
    if (request.getCourses() != null) {
      for (StudentCourse course : request.getCourses()) {
        // course_idを明示的にセット
        course.setCourseId(UUID.randomUUID().toString());
        course.setStudentId(request.getStudent().getStudentId()); // student_id もセット
        repository.insertCourse(course);
      }
    }
  }
}
