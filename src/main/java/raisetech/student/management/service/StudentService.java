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
 * 受講生情報のビジネスロジックを管理するサービスクラス。
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
   * 受講生とそのコース情報を登録します。
   *
   * @param student 受講生エンティティ
   * @param courses 受講生に紐づくコース情報のリスト
   */
  @Transactional
  public void registerStudent(Student student, List<StudentCourse> courses) {
    repository.insertStudent(student);
    for (StudentCourse course : courses) {
      courseRepository.insertCourse(course);
    }
  }

  /**
   * 受講生情報とコース情報を全体更新します。
   *
   * @param student 更新対象の受講生エンティティ
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
   * 受講生情報を部分的に更新します。
   *
   * @param student 部分更新後の受講生データ
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
   * 受講生IDで受講生情報を取得します。
   *
   * @param studentId 検索対象の受講生ID
   * @return 該当する受講生情報（存在しない場合は null）
   */
  public Student findStudentById(String studentId) {
    Student student = repository.findById(studentId);
    if (student == null) {
      throw new ResourceNotFoundException("受講生ID " + studentId + " が見つかりません。");
    }
    return student;
  }

  /**
   * 論理削除されていないすべての受講生を取得します。
   *
   * @return アクティブな受講生情報のリスト
   */
  public List<Student> searchActiveStudents() {
    return repository.searchActiveStudents();
  }

  /**
   * 論理削除された受講生のみを取得します。
   *
   * @return 削除済み受講生のリスト
   */
  public List<Student> searchDeletedStudents() {
    return repository.findDeletedStudents();
  }

  /**
   * 論理削除されていない受講生から、ふりがなで部分一致検索します。
   *
   * @param furigana 検索するふりがな（部分一致）
   * @return 一致する受講生リスト
   */
  public List<Student> findStudentsByFurigana(String furigana) {
    return repository.findByFurigana(furigana);
  }

  /**
   * 論理削除された受講生も含めて、ふりがなで部分一致検索します。
   *
   * @param furigana 検索するふりがな
   * @return 一致する受講生情報のリスト
   */
  public List<Student> findStudentsByFuriganaIncludingDeleted(String furigana) {
    return repository.findByFuriganaIncludingDeleted(furigana);
  }

  /**
   * 指定されたふりがなに一致する削除済み受講生のみを取得します。
   *
   * @param furigana ふりがな（部分一致検索）
   * @return 条件に一致する削除済み受講生のリスト
   */
  public List<Student> findDeletedStudentsByFurigana(String furigana) {
    return repository.findDeletedStudentsByFurigana(furigana);
  }

  /**
   * 受講生IDに紐づくコース情報を取得します。
   *
   * @param studentId 受講生ID
   * @return その受講生に紐づくコース一覧
   */
  public List<StudentCourse> searchCoursesByStudentId(String studentId) {
    return courseRepository.findCoursesByStudentId(studentId);
  }

  /**
   * すべてのコース情報を取得します。
   *
   * @return 全受講生分のコース情報一覧
   */
  public List<StudentCourse> searchAllCourses() {
    return courseRepository.findAllCourses();
  }

  /**
   * すべての受講生（論理削除された受講生も含む）を取得します。
   *
   * @return 受講生エンティティのリスト
   */
  public List<Student> searchAllStudents() {
    return repository.findAllStudents(); // is_deleted 条件なし
  }


  /**
   * 指定した受講生を論理削除します。
   *
   * @param studentId 削除対象の受講生ID
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
   * @param studentId 復元対象の受講生ID
   */
  @Transactional
  public void restoreStudent(String studentId) {
    Student student = repository.findById(studentId);
    if (student == null) {
      throw new ResourceNotFoundException("受講生ID " + studentId + " が見つかりません。");
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
   * 指定された受講生IDに基づいて、関連する受講生コースと受講生情報を物理削除します。
   * <p>
   * 削除前に該当する受講生が存在するかどうかを確認し、存在しない場合は例外をスローします。
   *
   * @param studentId 削除対象の受講生ID
   * @throws ResourceNotFoundException 受講生が存在しない場合にスローされます。
   */
  public void deleteStudentPhysically(String studentId) {
    // 受講生が存在するか確認（存在しなければ例外スロー）
    Student existing = repository.findById(studentId);
    if (existing == null) {
      throw new ResourceNotFoundException("受講生ID " + studentId + " が見つかりません。");
    }
    // 受講生に紐づくコースを削除
    courseRepository.deleteCoursesByStudentId(studentId);
    // 受講生情報を削除
    repository.deleteById(studentId);
  }
}

