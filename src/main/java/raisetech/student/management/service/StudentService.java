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
    return repository.search();
  }


  public List<StudentCourse> searchCourseList() {
    // 全てのコースリストを取得
    return repository.findAllCourses();
  }


  public List<StudentCourse> searchCoursesByStudentId(String studentId) {
    // studentIdに紐付くコースリストを取得
    return repository.findCoursesByStudentId(studentId);
  }

  public Student findStudentById(String studentId) {
    // studentIdで特定の生徒を探す
    return repository.findStudentById(studentId);
  }


  public List<Student> findStudentsByFurigana(String furigana) {
    // student.furiganaで特定の生徒を探す
    return repository.findStudentsByFurigana(furigana);
  }

  // @Transactional をつけることで、処理がすべて成功しないとデータが保存されないようにする。
  @Transactional
  public void registerStudentWithCourses(StudentRegistrationRequest request) {
    // student_id が null または空の場合のみ、新しい UUID を生成
    if (request.getStudent().getStudentId() == null || request.getStudent().getStudentId()
        .isEmpty()) {
      request.getStudent().setStudentId(UUID.randomUUID().toString());
    }

    // 学生情報の登録
    repository.insertStudent(request.getStudent());

    // コース情報の登録
    if (request.getCourses() != null) {
      for (StudentCourse course : request.getCourses()) {
        // course_idを明示的にセット
        course.setCourseId(UUID.randomUUID().toString());
        course.setStudentId(request.getStudent().getStudentId()); // student_id をセット
        repository.insertCourse(course);
      }
    }
  }

  @Transactional
  public void updateStudentWithCourses(StudentRegistrationRequest request) {
    // まず既存のコースを削除
    repository.deleteCoursesByStudentId(request.getStudent().getStudentId());

    // その後、学生情報を更新
    repository.updateStudent(request.getStudent());

    // コース情報の再登録
    if (request.getCourses() != null) {
      for (StudentCourse course : request.getCourses()) {
        course.setCourseId(UUID.randomUUID().toString());
        course.setStudentId(request.getStudent().getStudentId());
        repository.insertCourse(course);
      }
    }
  }
}
