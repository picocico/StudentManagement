package raisetech.student.management.service;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 *
 * <p>受講生およびコース情報の登録・更新・削除・検索といったビジネスロジックを提供します。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

  private final StudentRepository studentRepository;
  private final StudentCourseRepository courseRepository;
  private final StudentConverter converter;

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
    // 1. まず学生情報を更新
    int updated = studentRepository.updateStudent(student);
    // 2. 1件も更新されなければ「存在しないID」とみなして404系例外を投げる
    if (updated == 0) {
      String idForLog = converter.encodeUuidString(student.getStudentId());
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }
    // 3. 学生の更新が成功した場合だけ、コース側を更新
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
    Objects.requireNonNull(student, "student must not be null");

    byte[] studentId = student.getStudentId();
    if (studentId == null || studentId.length != 16) {
      throw new IllegalArgumentException("UUIDの形式が不正です");
    }

    int updated = studentRepository.updateStudent(student);
    if (updated == 0) {
      String idForLog = converter.encodeUuidString(studentId);
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }

    if (courses != null && !courses.isEmpty()) {
      // 念のため studentId を統一
      for (StudentCourse c : courses) {
        c.setStudentId(studentId);
      }
      courseRepository.deleteCoursesByStudentId(studentId);
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
   *
   * <p>このメソッドでは、氏名、メールアドレス、年齢などの基本属性のみが更新対象となり、 コース情報（student_coursesテーブル）は一切変更されません。
   *
   * <p>PATCHリクエストで「コースの追加」のみを行う場合に併用され、 既存のコース情報を保持したまま、受講生の属性情報だけを変更したいケースで使用します。
   *
   * @param student 更新対象の受講生エンティティ（student_idを含む必要があります）
   */
  @Override
  @Transactional
  public void updateStudentInfoOnly(Student student) {
    Objects.requireNonNull(student, "student must not be null");

    byte[] studentId = student.getStudentId();
    if (studentId == null || studentId.length != 16) {
      throw new IllegalArgumentException("UUIDの形式が不正です");
    }

    int updated = studentRepository.updateStudent(student);
    if (updated == 0) {
      String idForLog = converter.encodeUuidString(studentId);
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }
  }

  /**
   * 検索条件に基づいて受講生詳細情報リストを取得します。
   *
   * @param furigana       ふりがな検索（省略可能）
   * @param includeDeleted 論理削除済みも含めるか
   * @param deletedOnly    論理削除済みのみ取得するか
   * @return 受講生詳細DTOリスト
   */
  @Override
  public List<StudentDetailDto> getStudentList(
      String furigana, boolean includeDeleted, boolean deletedOnly) {
    log.debug(
        "Searching students with furigana={}, includeDeleted={}, deletedOnly={}",
        furigana,
        includeDeleted,
        deletedOnly);
    if (includeDeleted && deletedOnly) {
      throw new IllegalArgumentException(
          "includeDeletedとdeletedOnlyの両方をtrueにすることはできません");
    }
    // 動的SQLにより1本化されたリポジトリメソッドを呼び出し
    List<Student> students =
        studentRepository.searchStudents(furigana, includeDeleted, deletedOnly); // 1本化！
    List<StudentCourse> courses = searchAllCourses();
    return converter.toDetailDtoList(students, courses);
  }

  /**
   * 受講生IDで受講生情報を取得します。
   *
   * @param studentId 受講生ID（UUID を BINARY(16) で保持した 16バイト配列）
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
      // ログ用に UUID 文字列を生成
      String idForLog = converter.encodeUuidString(studentId);
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }
    return student;
  }

  /**
   * 受講生IDに紐づくコース情報を取得します。
   *
   * @param studentId 受講生ID（UUID を BINARY(16) で保持した 16バイト配列）
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
   * @param studentId 受講生ID（UUID を BINARY(16) で保持した 16バイト配列）
   */
  @Override
  @Transactional
  public void softDeleteStudent(byte[] studentId) {
    Student student = studentRepository.findById(studentId);

    // 対象の受講生が存在しない場合は例外をスロー
    if (student == null) {
      throw new ResourceNotFoundException(
          "Student not found for ID: " + converter.encodeUuidString(studentId));
    }

    // すでに論理削除済みでなければ、削除処理を行う
    if (!Boolean.TRUE.equals(student.getDeleted())) {
      student.softDelete();
      int updated = studentRepository.updateStudent(student);
      if (updated == 0) {
        // ここは通常起こりにくいが、整合性の保険として
        throw new IllegalStateException("論理削除に失敗しました: " +
            converter.encodeUuidString(studentId));
      }
      log.info("論理削除完了 - studentId: {}", converter.encodeUuidString(studentId));
    }
  }

  /**
   * 論理削除された受講生を復元します。
   *
   * @param studentId 受講生ID（UUID を BINARY(16) で保持した 16バイト配列）
   * @throws ResourceNotFoundException 受講生が存在しない場合
   */
  @Override
  @Transactional
  public void restoreStudent(byte[] studentId) {
    Student student = studentRepository.findById(studentId);
    String idForLog = converter.encodeUuidString(studentId);

    if (student == null) {
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }

    log.debug(
        "Before restore: studentId = {}, deleted = {}, deletedAt = {}",
        idForLog,
        student.getDeleted(),
        student.getDeletedAt());

    if (Boolean.TRUE.equals(student.getDeleted())) {
      student.restore();
      int updated = studentRepository.updateStudent(student);
      log.debug(
          "After restore: studentId = {}, deleted = {}, deletedAt = {}",
          idForLog,
          student.getDeleted(),
          student.getDeletedAt());
      if (updated == 0) {
        throw new IllegalStateException("復元に失敗しました: " + idForLog);
      }
    }
  }

  /**
   * 指定された受講生IDに該当する受講生情報および関連するコース情報を物理削除します。
   *
   * <p>この操作はデータベースから完全に削除され、復元はできません。 主に管理者向けの操作として利用されます。
   *
   * @param studentId 物理削除対象の受講生ID（UUIDをBINARY(16)型で格納した16バイトの配列）
   * @throws ResourceNotFoundException 該当する受講生が存在しない場合にスローされます
   */
  @Override
  @Transactional
  public void forceDeleteStudent(byte[] studentId) {
    String idForLog = converter.encodeUuidString(studentId);
    // 1. 先にコースを削除（存在しないIDなら 0件削除で終わるだけ）
    courseRepository.deleteCoursesByStudentId(studentId);

    // 2. 物理削除して、削除件数を受け取る
    int deleted = studentRepository.forceDeleteStudent(studentId);

    // 3. 1件も削除されなければ「存在しないID」と判断
    if (deleted == 0) {
      // Transactional によりコース削除もロールバックされるので、副作用は残らない
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }

    // 4. 正常に1件削除された場合はログを出して終了
    log.info("物理削除完了 - studentId: {}", idForLog);
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

    // 1) いきなり UPDATE して件数を見る
    int updatedCount = studentRepository.updateStudent(student);
    if (updatedCount == 0) {
      String idForLog = converter.encodeUuidString(studentId);
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }

    // 2) コース全削除 → 一括Insert（メソッド名：deleteCoursesByStudentId / insertCourses）
    courseRepository.deleteCoursesByStudentId(studentId);
    if (courses != null && !courses.isEmpty()) {
      for (StudentCourse sc : courses) {
        sc.setStudentId(studentId); // 念のため上書き
      }
      courseRepository.insertCourses(courses);
    }

    // 3) 最新の学生を再取得（null返し仕様に合わせる）
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

  @Override
  @Transactional
  public void replaceCourses(byte[] studentIdBytes, List<StudentCourse> newCourses) {
    // 受講生の存在チェック（必要なら既存メソッド呼び出し）
    findStudentById(studentIdBytes);

    // 既存コースを全削除
    courseRepository.deleteCoursesByStudentId(studentIdBytes);

    // 新規があれば挿入
    if (newCourses != null && !newCourses.isEmpty()) {
      for (StudentCourse c : newCourses) {
        c.setStudentId(studentIdBytes); // 念のためセット
      }
      courseRepository.insertCourses(newCourses);
    }
  }
}
