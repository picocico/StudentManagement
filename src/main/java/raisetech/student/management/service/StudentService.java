package raisetech.student.management.service;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.dto.StudentWithCoursesDTO;
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
    return repository.search();
  }

  public List<StudentCourse> searchCourseList() {
    // 全てのコースリストを取得
    List<StudentCourse> courses = repository.findAllCourses();
    return repository.findAllCourses();
  }

  public List<StudentCourse> searchCoursesByStudentId(String studentId) {
    // 指定されたstudentIdに基づいて、生徒が登録しているコース情報を取得
    return repository.findCoursesByStudentId(studentId);
  }

  public Student findStudentByStudentId(String studentId) {
    // 指定されたstudentIdに基づいて、生徒の登録情報を取得
    Student student = repository.findStudentByStudentId(studentId);
    if (student == null) {
      System.out.println("Student not found for ID: " + studentId);
    }
    return student;
  }

  public StudentWithCoursesDTO findStudentWithCourses(String studentId) {
    // 指定されたstudentIdに基づいて、生徒登録情報およびコース情報を取得
    Student student = repository.findStudentByStudentId(studentId);
    if (student == null) {
      return null;
    }

    List<StudentCourse> courses = repository.findCoursesByStudentId(studentId);
    return new StudentWithCoursesDTO(student, courses);
  }

  // @Transactional をつけることで、処理がすべて成功しないとデータが保存されないようにする。
  @Transactional
  public void registerStudentWithCourses(StudentRegistrationRequest request) {
    // TODO:student_id を明示的にセット
    request.getStudent().setStudentId(UUID.randomUUID().toString());
    // TODO:`students` に登録
    repository.insertStudent(request.getStudent());

    // TODO:コース情報の登録 `students` に登録されてから `student_courses` に登録
    if (request.getCourses() != null && !request.getCourses().isEmpty()) {
      for (StudentCourse course : request.getCourses()) {
        // course_idを明示的にセット
        course.setCourseId(UUID.randomUUID().toString()); // course_id をセット
        course.setStudentId(request.getStudent().getStudentId()); // MySQLで生成されたstudent_id をセット
        repository.insertCourse(course);
      }
    }
  }
}
