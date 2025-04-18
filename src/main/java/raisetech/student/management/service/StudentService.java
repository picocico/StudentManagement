package raisetech.student.management.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.exception.ResourceNotFoundException;
import raisetech.student.management.repository.StudentCourseRepository;
import raisetech.student.management.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 学生情報のビジネスロジックを管理するサービスクラス。
 * 登録・更新・検索・削除などの操作を提供する。
 */
@Service
public class StudentService {

  private static final Logger logger = LoggerFactory.getLogger(StudentService.class);

  private final StudentRepository repository;
  private final StudentCourseRepository courseRepository;

  @Autowired
  public StudentService(StudentRepository repository, StudentCourseRepository courseRepository) {
    this.repository = repository;
    this.courseRepository = courseRepository;
  }

  /**
   * 学生とそのコース情報を登録します。
   *
   * @param student 学生エンティティ
   * @param courses 学生に紐づくコース情報のリスト
   */
  @Transactional
  public void registerStudent(Student student, List<StudentCourse> courses) {
    repository.insertStudent(student);
    for (StudentCourse course : courses) {
      courseRepository.insertCourse(course);
    }
  }

  /**
   * 学生情報とコース情報を全体更新します。
   *
   * @param student 更新対象の学生エンティティ
   * @param courses 新しいコース情報のリスト
   */
  @Transactional
  public void updateStudent(Student student, List<StudentCourse> courses) {
    repository.updateStudent(student);
    courseRepository.deleteCoursesByStudentId(student.getStudentId());
    for (StudentCourse course : courses) {
      courseRepository.insertCourse(course);
    }
  }

  /**
   * 学生情報を部分的に更新します。
   *
   * @param student 部分更新後の学生データ
   * @param courses コース情報（省略可能）
   */
  @Transactional
  public void partialUpdateStudent(Student student, List<StudentCourse> courses) {
    repository.updateStudent(student);
    if (courses != null && !courses.isEmpty()) {
      courseRepository.deleteCoursesByStudentId(student.getStudentId());
      for (StudentCourse course : courses) {
        courseRepository.insertCourse(course);
      }
    }
  }

  /**
   * 学生IDで学生情報を取得します。
   *
   * @param studentId 検索対象の学生ID
   * @return 該当する学生情報（存在しない場合は null）
   */
  public Student findStudentById(String studentId) {
    Student student = repository.findById(studentId);
    if (student == null) {
      throw new ResourceNotFoundException("学生ID " + studentId + " が見つかりません。");
    }
    return student;
  }

  /**
   * 削除されていないすべての学生を取得します。
   *
   * @return アクティブな学生情報のリスト
   */
  public List<Student> searchActiveStudents() {
    return repository.searchActiveStudents();
  }

  /**
   * 指定したふりがなに一致する学生を検索します。
   *
   * @param furigana 検索キーワード（部分一致）
   * @return 一致する学生リスト
   */
  public List<Student> findStudentsByFurigana(String furigana) {
    return repository.findByFurigana(furigana);
  }

  /**
   * 学生IDに紐づくコース情報を取得します。
   *
   * @param studentId 学生ID
   * @return その学生に紐づくコース一覧
   */
  public List<StudentCourse> searchCoursesByStudentId(String studentId) {
    return courseRepository.findCoursesByStudentId(studentId);
  }

  /**
   * すべてのコース情報を取得します。
   *
   * @return 全学生分のコース情報一覧
   */
  public List<StudentCourse> searchAllCourses() {
    return courseRepository.findAllCourses();
  }

  /**
   * すべての学生（論理削除された学生も含む）を取得します。
   *
   * @return 学生エンティティのリスト
   */
  public List<Student> searchAllStudents() {
    return repository.findAllStudents(); // is_deleted 条件なし
  }


  /**
   * 指定した学生を論理削除します。
   *
   * @param studentId 削除対象の学生ID
   */
  @Transactional
  public void softDeleteStudent(String studentId) {
    Student student = repository.findById(studentId);
    if (student != null && !Boolean.TRUE.equals(student.getDeleted())) {
      student.softDelete();
      repository.updateStudent(student);
    }
  }

  /**
   * 指定した学生を論理削除から復元します。
   *
   * @param studentId 復元対象の学生ID
   */
  @Transactional
  public void restoreStudent(String studentId) {
    Student student = repository.findById(studentId);
    if (student == null) {
      throw new ResourceNotFoundException("学生ID " + studentId + " が見つかりません。");
    }

    logger.debug("Before restore: deleted = {}, deletedAt = {}", student.getDeleted(), student.getDeletedAt());

    // 論理削除されている場合のみ復元（nullの場合も含む）
    if (Boolean.TRUE.equals(student.getDeleted())) {
      student.restore(); // ← deleted=false, deletedAt=null
      logger.debug("After restore: deleted = {}, deletedAt = {}", student.getDeleted(), student.getDeletedAt());
      repository.updateStudent(student);
    }
  }

  /**
   * 指定された学生IDに基づいて、関連する学生コースと学生情報を物理削除します。
   * <p>
   * 削除前に該当する学生が存在するかどうかを確認し、存在しない場合は例外をスローします。
   *
   * @param studentId 削除対象の学生ID
   * @throws ResourceNotFoundException 学生が存在しない場合にスローされます。
   */
  public void deleteStudentPhysically(String studentId) {
    // 学生が存在するか確認（存在しなければ例外スロー）
    Student existing = repository.findById(studentId);
    if (existing == null) {
      throw new ResourceNotFoundException("学生ID " + studentId + " が見つかりません。");
    }
    // 学生に紐づくコースを削除
    courseRepository.deleteCoursesByStudentId(studentId);
    // 学生情報を削除
    repository.deleteById(studentId);
  }
}

