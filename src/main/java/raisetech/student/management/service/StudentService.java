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
import raisetech.student.management.domain.StudentDetail;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.repository.StudentRepository;

@Service
public class StudentService {

  private final StudentRepository repository;

  @Autowired
  public StudentService(StudentRepository repository) {
    this.repository = repository;
  }

  // 論理削除されていない受講生リストを取得
  public List<StudentDetail> searchActiveStudentList() {
    List<Student> students = repository.searchActiveStudents(); // is_deleted = false
    return students.stream()
        .map(student -> {
          List<StudentCourse> courses = repository.findCoursesByStudentId(student.getStudentId());
          return new StudentDetail(student, courses);
        })
        .toList();
  }

  // 受講生のリストを取得（論理削除も含む）
  public List<StudentDetail> searchStudentList() {
    List<Student> students = repository.search();
    return students.stream()
        .map(student -> {
          List<StudentCourse> courses = repository.findCoursesByStudentId(student.getStudentId());
          return new StudentDetail(student, courses);
        })
        .toList();
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
    logger.debug("Before set: student.isDeleted = {}", request.getStudent().getDeleted());

// 削除フラグを Student オブジェクトに反映
    request.getStudent().setDeleted(request.isDeleted());

// ログ③: セット後のStudentオブジェクトの削除フラグ
    logger.debug("After set: student.isDeleted = {}", request.getStudent().getDeleted());

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

  @Transactional
  public void partialUpdateStudentWithCourses(StudentRegistrationRequest request) {

    Logger logger = LoggerFactory.getLogger(StudentService.class);

    Student existing = repository.findStudentById(request.getStudent().getStudentId());
    if (existing == null) {
      throw new RuntimeException("受講生情報が存在しません");
    }

    logger.debug("PATCH: Before update - isDeleted = {}", existing.getDeleted());

    // 名前の変更
    if (request.getStudent().getFullName() != null) {
      existing.setFullName(request.getStudent().getFullName());
    }

    // ふりがなの変更
    if (request.getStudent().getFurigana() != null) {
      existing.setFurigana(request.getStudent().getFurigana());
    }

    // ニックネームの変更
    if (request.getStudent().getNickname() != null) {
      existing.setNickname(request.getStudent().getNickname());
    }

    // emailの変更
    if (request.getStudent().getEmail() != null) {
      existing.setEmail(request.getStudent().getEmail());
    }

    // 居住地域の変更
    if (request.getStudent().getLocation() != null) {
      existing.setLocation(request.getStudent().getLocation());
    }

    // 年齢の変更
    if (request.getStudent().getAge() != null) {
      existing.setAge(request.getStudent().getAge());
    }

    // 性別の変更
    if (request.getStudent().getGender() != null) {
      existing.setGender(request.getStudent().getGender());
    }

    // 備考欄の変更
    if (request.getStudent().getRemarks() != null) {
      existing.setRemarks(request.getStudent().getRemarks());
    }

    // 論理削除の変更
    if (request.getStudent().getDeleted() != null) {
      if (request.getStudent().getDeleted()) {
        existing.softDelete(); // ← フラグと日時を両方設定
      } else {
        existing.restore(); // ← 復元
      }
    }

    repository.updateStudent(existing);

    // コースの部分更新もする場合（指定があるときだけ）
    if (request.getCourses() != null && !request.getCourses().isEmpty()) {
      repository.deleteCoursesByStudentId(existing.getStudentId());

      for (StudentCourse course : request.getCourses()) {
        course.setCourseId(UUID.randomUUID().toString());
        course.setStudentId(existing.getStudentId());
        repository.insertCourse(course);
      }
    }
  }

  @Transactional
  public void deleteStudent(String studentId) {
    repository.deleteCoursesByStudentId(studentId); // まず関連コースを削除
    repository.deleteStudentById(studentId);        // 次に生徒を削除
  }
}
