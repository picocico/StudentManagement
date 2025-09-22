package raisetech.student.management.service;

import java.util.List;
import java.util.Objects;
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

  private final StudentRepository studentRepository;
  private final StudentCourseRepository courseRepository;
  private final StudentConverter converter;


  private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);

  /**
   * 受講生を登録します。
   *
   * @param student 登録する受講生エンティティ
   * @param courses 受講生に紐づくコースリスト（複数可）
   */
  @Override
  @Transactional
  public void registerStudent(Student student, List<StudentCourse> courses) {
    studentRepository.insertStudent(student);
    if (courses != null && !courses.isEmpty()) {
      courseRepository.insertCourses(courses);
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
    studentRepository.updateStudent(student);
    courseRepository.deleteCoursesByStudentId(student.getStudentId());
    if (courses != null && !courses.isEmpty()) {
      courseRepository.insertCourses(courses);
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
    studentRepository.updateStudent(student);
    if (courses != null && !courses.isEmpty()) {
      courseRepository.deleteCoursesByStudentId(student.getStudentId());
      courseRepository.insertCourses(courses);
    }
  }

  /**
   * 既存の受講生に新しいコースのみを追加します（既存のコースは保持）。
   *
   * @param studentId  受講生ID
   * @param newCourses 追加するコースリスト
   */
  public void appendCourses(byte[] studentId, List<StudentCourse> newCourses) {
    for (StudentCourse course : newCourses) {
      courseRepository.insertIfNotExists(course); // 存在しないときだけinsert
    }
  }

  /**
   * 受講生の基本情報のみを更新します。
   * <p>
   * このメソッドでは、氏名、メールアドレス、年齢などの基本属性のみが更新対象となり、
   * コース情報（student_coursesテーブル）は一切変更されません。
   * <p>
   * PATCHリクエストで「コースの追加」のみを行う場合に併用され、
   * 既存のコース情報を保持したまま、受講生の属性情報だけを変更したいケースで使用します。
   *
   * @param student 更新対象の受講生エンティティ（student_idを含む必要があります）
   */
  @Override
  @Transactional
  public void updateStudentInfoOnly(Student student) {
    studentRepository.updateStudent(student);
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
    if (includeDeleted && deletedOnly) {
      throw new IllegalArgumentException("includeDeletedとdeletedOnlyの両方をtrueにすることはできません");
    }
    // 動的SQLにより1本化されたリポジトリメソッドを呼び出し
    List<Student> students = studentRepository.searchStudents(furigana, includeDeleted, deletedOnly); // 1本化！
    List<StudentCourse> courses = searchAllCourses();
    return converter.toDetailDtoList(students, courses);
  }

  /**
   * 受講生IDで受講生情報を取得します。
   *
   * @param studentId 受講生ID（BINARY型、Base64エンコード前の16バイト配列）
   * @return 該当する受講生
   * @throws ResourceNotFoundException 該当する受講生が存在しない場合
   */
  @Override
  public Student findStudentById(byte[] studentId) {
    if (studentId == null || studentId.length != 16) {
      throw new IllegalArgumentException("UUIDの形式が不正です");
    }

    Student student = studentRepository.findById(studentId);
    if (student == null) {
      String idForLog = converter.encodeBase64(studentId);
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }
    return student;
  }

  /**
   * 受講生IDに紐づくコース情報を取得します。
   *
   * @param studentId 受講生ID（BINARY型、Base64エンコード前の16バイト配列）
   * @return コースリスト
   */
  @Override
  public List<StudentCourse> searchCoursesByStudentId(byte[] studentId) {
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
   * @param studentId 受講生ID（BINARY型、Base64エンコード前の16バイト配列）
   */
  @Override
  @Transactional
  public void softDeleteStudent(byte[] studentId) {
    Student student = studentRepository.findById(studentId);

    // 対象の受講生が存在しない場合は例外をスロー
    if (student == null) {
      throw new ResourceNotFoundException("Student not found for ID: " + converter.encodeBase64(studentId));
    }

    // すでに論理削除済みでなければ、削除処理を行う
    if (!Boolean.TRUE.equals(student.getDeleted())) {
      student.softDelete();
      studentRepository.updateStudent(student);
      logger.info("論理削除完了 - studentId: {}", converter.encodeBase64(studentId));
    }
  }

  /**
   * 論理削除された受講生を復元します。
   *
   * @param studentId 受講生ID（BINARY型、Base64エンコード前の16バイト配列）
   * @throws ResourceNotFoundException 受講生が存在しない場合
   */
  @Override
  @Transactional
  public void restoreStudent(byte[] studentId) {
    Student student = studentRepository.findById(studentId);
    String idForLog = converter.encodeBase64(studentId);

    if (student == null) {
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }

    logger.debug("Before restore: studentId = {}, deleted = {}, deletedAt = {}",
        idForLog,student.getDeleted(), student.getDeletedAt());

    if (Boolean.TRUE.equals(student.getDeleted())) {
      student.restore();
      studentRepository.updateStudent(student);
      logger.debug("After restore: studentId = {}, deleted = {}, deletedAt = {}",
          idForLog,student.getDeleted(), student.getDeletedAt());
    }
  }

  /**
   * 指定された受講生IDに該当する受講生情報および関連するコース情報を物理削除します。
   * <p>
   * この操作はデータベースから完全に削除され、復元はできません。
   * 主に管理者向けの操作として利用されます。
   *
   * @param studentId 物理削除対象の受講生ID（UUIDをBINARY(16)型で格納した16バイトの配列）
   * @throws ResourceNotFoundException 該当する受講生が存在しない場合にスローされます
   */
  @Override
  @Transactional
  public void forceDeleteStudent(byte[] studentId) {
    String idForLog = converter.encodeBase64(studentId);
    // 存在確認
    if (studentRepository.findById(studentId) == null) {
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }
    // 関連するコースも削除
    courseRepository.deleteCoursesByStudentId(studentId);
    // 学生レコードの物理削除
    studentRepository.forceDeleteStudent(studentId);
    logger.info("物理削除完了 - studentId: {}", idForLog);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public Student updateStudentWithCourses(Student student, List<StudentCourse> courses) {
    Objects.requireNonNull(student, "student must not be null");
    byte[] studentId = student.getStudentId();
    if (studentId == null) {
      throw new IllegalArgumentException("studentId must not be null");
    }

    // 1) 存在確認（StudentRepository#findById は null返し仕様）
    Student present = studentRepository.findById(studentId);
    if (present == null) {
      throw new ResourceNotFoundException("student", "studentId");
    }

    // 2) 学生本体の更新（メソッド名は updateStudent）
    studentRepository.updateStudent(student);

    // 3) コース全削除 → 一括Insert（メソッド名：deleteCoursesByStudentId / insertCourses）
    courseRepository.deleteCoursesByStudentId(studentId);
    if (courses != null && !courses.isEmpty()) {
      for (StudentCourse sc : courses) {
        sc.setStudentId(studentId); // 念のため上書き
      }
      courseRepository.insertCourses(courses);
    }

    // 4) 最新の学生を再取得（null返し仕様に合わせる）
    Student updated = studentRepository.findById(studentId);
    if (updated == null) {
      // 直前で更新しているので通常起きないが、整合性確保のため
      throw new ResourceNotFoundException("student", "studentId");
    }
    return updated;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<StudentCourse> getCoursesByStudentId(byte[] studentId) {
    if (studentId == null) {
      throw new IllegalArgumentException("studentId must not be null");
    }
    // メソッド名：findCoursesByStudentId
    return courseRepository.findCoursesByStudentId(studentId);
  }
}




