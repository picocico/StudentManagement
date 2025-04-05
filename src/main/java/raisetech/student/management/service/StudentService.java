package raisetech.student.management.service;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  // 論理削除されていない生徒を取得するメソッドを追加
  public List<Student> searchActiveStudents() {
    return repository.searchActiveStudents();
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

  // フル更新（フォームから courses を受け取ったときだけ呼ぶ）
  @Transactional
  public void updateStudentWithCourses(StudentRegistrationRequest request) {

    Logger logger = LoggerFactory.getLogger(StudentService.class);

// ログ①: フォームから受け取った削除フラグ
    logger.debug("request.isDeleted() = {}", request.isDeleted());

// ログ②: セット前のStudentオブジェクトの削除フラグ
    logger.debug("Before set: student.isDeleted = {}", request.getStudent().isDeleted());

// 削除フラグを Student オブジェクトに反映
    request.getStudent().setDeleted(request.isDeleted());

// ログ③: セット後のStudentオブジェクトの削除フラグ
    logger.debug("After set: student.isDeleted = {}", request.getStudent().isDeleted());


    repository.updateStudent(request.getStudent());

    if (request.getCourses() != null && !request.getCourses().isEmpty()) {
      repository.deleteCoursesByStudentId(request.getStudent().getStudentId());
      for (StudentCourse course : request.getCourses()) {
        course.setCourseId(UUID.randomUUID().toString());
        course.setStudentId(request.getStudent().getStudentId());
        repository.insertCourse(course);
      }
    }
  }
}
