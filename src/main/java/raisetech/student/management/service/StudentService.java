package raisetech.student.management.service;

import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.domain.ApplicationStatus;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.exception.ResourceNotFoundException;

/**
 * 受講生管理に関するビジネスロジックを提供するサービスインターフェース。
 *
 * <p>このインターフェースは、受講生の登録・更新・削除・検索などの操作を定義します。
 */
public interface StudentService {

  /**
   * 受講生情報とコース受講情報を一括で更新します（全置換）。
   *
   * <p>- 学生本体（氏名等）の更新<br>
   * - 既存の受講コースを全削除 ⇒ 引数のコース一覧に置き換え
   *
   * @param student 更新対象の学生エンティティ（studentId は必須）
   * @param courses 更新後に紐づける受講コースの一覧（null/空配列は「0件に置換」）
   * @return 更新後の学生エンティティ
   * @throws IllegalArgumentException studentId が null の場合
   * @throws ResourceNotFoundException 指定IDの学生が存在しない場合
   */
  @Transactional
  Student updateStudentWithCourses(Student student, List<StudentCourse> courses);

  /**
   * 条件に基づいて受講生詳細情報を取得します。
   *
   * @param furigana ふりがなによる検索（部分一致、null または空文字の場合は無視）
   * @param includeDeleted 削除済みデータを含めるかどうか
   * @param deletedOnly 削除済みデータのみ取得するかどうか
   * @return 受講生詳細情報のリスト
   */
  List<StudentDetailDto> getStudentList(
      String furigana,
      boolean includeDeleted,
      boolean deletedOnly,
      ApplicationStatus applicationStatus);

  /**
   * 受講生情報とそのコース情報を登録します。
   *
   * @param student 受講生エンティティ
   * @param courses コース情報のリスト
   */
  void registerStudent(Student student, List<StudentCourse> courses);

  StudentDetailDto patchStudent(
      UUID studentId, StudentRegistrationRequest req, String studentIdString);

  /**
   * 受講生IDにより受講生情報を取得します。
   *
   * @param studentId 受講生ID（UUID）
   * @return 受講生エンティティ
   */
  Student findStudentById(UUID studentId);

  /**
   * 受講生IDに紐づくコース情報を取得します。
   *
   * @param studentId 受講生ID（UUID）
   * @return コースエンティティのリスト
   */
  List<StudentCourse> searchCoursesByStudentId(UUID studentId);

  /**
   * 受講生を論理削除します。
   *
   * @param studentId 受講生ID（UUID）
   */
  void softDeleteStudent(UUID studentId);

  /**
   * 論理削除された受講生を復元します。
   *
   * @param studentId 受講生ID（UUID）
   */
  void restoreStudent(UUID studentId);

  /**
   * 受講生情報とそのコース情報を物理削除します（管理者専用）。
   *
   * @param studentId 削除対象の受講生ID（UUID）
   */
  void forceDeleteStudent(UUID studentId);
}
