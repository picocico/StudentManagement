package raisetech.student.management.service;

import java.util.List;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentDetailDto;

/**
 * 受講生管理に関するビジネスロジックを提供するサービスインターフェース。
 * <p>
 * このインターフェースは、受講生の登録・更新・削除・検索などの操作を定義します。
 */
public interface StudentService {

  /**
   * 条件に基づいて受講生詳細情報を取得します。
   *
   * @param furigana        ふりがなによる検索（部分一致、null または空文字の場合は無視）
   * @param includeDeleted  削除済みデータを含めるかどうか
   * @param deletedOnly     削除済みデータのみ取得するかどうか
   * @return 受講生詳細情報のリスト
   */
  List<StudentDetailDto> getStudentList(String furigana, boolean includeDeleted, boolean deletedOnly);

  /**
   * 受講生情報とそのコース情報を登録します。
   *
   * @param student 受講生エンティティ
   * @param courses コース情報のリスト
   */
  void registerStudent(Student student, List<StudentCourse> courses);

  /**
   * 受講生情報とそのコース情報を全体更新します。
   *
   * @param student 更新対象の受講生エンティティ
   * @param courses 新しいコース情報のリスト
   */
  void updateStudent(Student student, List<StudentCourse> courses);

  /**
   * 受講生情報を部分的に更新します。
   *
   * @param student 更新対象の受講生エンティティ（部分更新）
   * @param courses 更新対象のコース情報（null または空リストの場合はコースは変更しない）
   */
  void partialUpdateStudent(Student student, List<StudentCourse> courses);

  /**
   * 全受講生情報を取得します（論理削除されたデータも含む）。
   *
   * @return 受講生エンティティのリスト
   */
  List<Student> searchAllStudents();

  /**
   * 論理削除されていない受講生情報を取得します。
   *
   * @return アクティブな受講生エンティティのリスト
   */
  List<Student> searchActiveStudents();

  /**
   * 論理削除された受講生情報のみを取得します。
   *
   * @return 削除済みの受講生エンティティのリスト
   */
  List<Student> searchDeletedStudents();

  /**
   * ふりがなによる検索で、論理削除されていない受講生を取得します。
   *
   * @param furigana 検索するふりがな
   * @return 該当する受講生エンティティのリスト
   */
  List<Student> findStudentsByFurigana(String furigana);

  /**
   * ふりがなによる検索で、論理削除された受講生も含めて取得します。
   *
   * @param furigana 検索するふりがな
   * @return 該当する受講生エンティティのリスト
   */
  List<Student> findStudentsByFuriganaIncludingDeleted(String furigana);

  /**
   * ふりがなによる検索で、論理削除された受講生のみを取得します。
   *
   * @param furigana 検索するふりがな
   * @return 該当する削除済み受講生エンティティのリスト
   */
  List<Student> findDeletedStudentsByFurigana(String furigana);

  /**
   * 受講生IDにより受講生情報を取得します。
   *
   * @param studentId 受講生ID
   * @return 受講生エンティティ
   */
  Student findStudentById(String studentId);

  /**
   * 受講生IDに紐づくコース情報を取得します。
   *
   * @param studentId 受講生ID
   * @return コースエンティティのリスト
   */
  List<StudentCourse> searchCoursesByStudentId(String studentId);

  /**
   * 全てのコース情報を取得します。
   *
   * @return コースエンティティのリスト
   */
  List<StudentCourse> searchAllCourses();

  /**
   * 受講生を論理削除します。
   *
   * @param studentId 受講生ID
   */
  void softDeleteStudent(String studentId);

  /**
   * 論理削除された受講生を復元します。
   *
   * @param studentId 受講生ID
   */
  void restoreStudent(String studentId);

  /**
   * 受講生情報とそのコース情報を物理削除します。
   *
   * @param studentId 受講生ID
   */
  void deleteStudentPhysically(String studentId);
}


