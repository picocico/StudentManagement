package raisetech.student.management.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.exception.ResourceNotFoundException;
import raisetech.student.management.repository.StudentCourseRepository;
import raisetech.student.management.repository.StudentRepository;

/**
 * {@link StudentService} の実装クラス
 * <p>
 * 受講生およびコース情報の登録・更新・削除・検索といったビジネスロジックを提供します。
 */
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

  private final StudentRepository repository;
  private final StudentCourseRepository courseRepository;
  private final StudentConverter converter;

  private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);

  /**
   * 受講生を登録します。
   *
   * @param student 登録する受講生エンティティ
   * @param courses 受講生に紐づくコースリスト
   */
  @Override
  @Transactional
  public void registerStudent(Student student, List<StudentCourse> courses) {
    repository.insertStudent(student);
    for (StudentCourse course : courses) {
      courseRepository.insertCourse(course);
    }
  }

  /**
   * 受講生とコース情報を全体更新します。
   *
   * @param student 更新対象の受講生エンティティ
   * @param courses 更新するコースリスト
   */
  @Override
  @Transactional
  public void updateStudent(Student student, List<StudentCourse> courses) {
    repository.updateStudent(student);
    courseRepository.deleteCoursesByStudentId(student.getStudentId());
    for (StudentCourse course : courses) {
      courseRepository.insertCourse(course);
    }
  }

  /**
   * 受講生とコース情報を部分更新します。
   *
   * @param student 部分更新対象の受講生エンティティ
   * @param courses 更新するコースリスト（省略可能）
   */
  @Override
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
   * 検索条件に基づいて受講生詳細情報リストを取得します。
   *
   * @param furigana        ふりがな検索（省略可能）
   * @param includeDeleted  論理削除済みも含めるか
   * @param deletedOnly     論理削除済みのみ取得するか
   * @return 受講生詳細DTOリスト
   */
  @Override
  public List<StudentDetailDto> getStudentList(String furigana, boolean includeDeleted, boolean deletedOnly) {
    logger.debug("Searching students with furigana={}, includeDeleted={}, deletedOnly={}",
        furigana, includeDeleted, deletedOnly);
    // 動的SQLにより1本化されたリポジトリメソッドを呼び出し
    List<Student> students = repository.searchStudents(furigana, includeDeleted, deletedOnly); // 1本化！
    List<StudentCourse> courses = searchAllCourses();

    return converter.convertStudentDetailsDto(students, courses);
  }

  /**
   * 受講生IDで受講生情報を取得します。
   *
   * @param studentId 受講生ID
   * @return 該当する受講生
   * @throws ResourceNotFoundException 該当する受講生が存在しない場合
   */
  @Override
  public Student findStudentById(String studentId) {
    Student student = repository.findById(studentId);
    if (student == null) {
      throw new ResourceNotFoundException("受講生ID " + studentId + " が見つかりません。");
    }
    return student;
  }

  /**
   * 受講生IDに紐づくコース情報を取得します。
   *
   * @param studentId 受講生ID
   * @return コースリスト
   */
  @Override
  public List<StudentCourse> searchCoursesByStudentId(String studentId) {
    return courseRepository.findCoursesByStudentId(studentId);
  }

  /**
   * 全コース情報を取得します。
   *
   * @return コースリスト
   */
  @Override
  public List<StudentCourse> searchAllCourses() {
    return courseRepository.findAllCourses();
  }

  /**
   * 受講生を論理削除します。
   *
   * @param studentId 受講生ID
   */
  @Override
  @Transactional
  public void softDeleteStudent(String studentId) {
    Student student = repository.findById(studentId);
    if (student != null && !Boolean.TRUE.equals(student.getDeleted())) {
      student.softDelete();
      repository.updateStudent(student);
    }
  }

  /**
   * 論理削除された受講生を復元します。
   *
   * @param studentId 受講生ID
   * @throws ResourceNotFoundException 受講生が存在しない場合
   */
  @Override
  @Transactional
  public void restoreStudent(String studentId) {
    Student student = repository.findById(studentId);
    if (student == null) {
      throw new ResourceNotFoundException("受講生ID " + studentId + " が見つかりません。");
    }

    logger.debug("Before restore: deleted = {}, deletedAt = {}", student.getDeleted(), student.getDeletedAt());

    if (Boolean.TRUE.equals(student.getDeleted())) {
      student.restore();
      logger.debug("After restore: deleted = {}, deletedAt = {}", student.getDeleted(), student.getDeletedAt());
      repository.updateStudent(student);
    }
  }

  /**
   * 受講生を物理削除します（関連コース情報も削除）。
   *
   * @param studentId 受講生ID
   * @throws ResourceNotFoundException 受講生が存在しない場合
   */
  @Override
  public void deleteStudentPhysically(String studentId) {
    Student existing = repository.findById(studentId);
    if (existing == null) {
      throw new ResourceNotFoundException("受講生ID " + studentId + " が見つかりません。");
    }
    courseRepository.deleteCoursesByStudentId(studentId);
    repository.deleteById(studentId);
  }
}


