package raisetech.student.management.service;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentDetailDto;
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
   * @throws IllegalArgumentException                                         studentId が null の場合
   * @throws raisetech.student.management.exception.ResourceNotFoundException 指定IDの学生が存在しない場合
   */
  @Transactional
  Student updateStudentWithCourses(Student student, List<StudentCourse> courses);

  /**
   * 学生IDに紐づく受講コース一覧を取得します。
   *
   * @param studentId 学生ID（BINARY(16)）
   * @return 受講コース一覧（0件の場合は空リスト）
   * @throws IllegalArgumentException studentId が null の場合
   */
  List<StudentCourse> getCoursesByStudentId(byte[] studentId);

  // 既存メソッド群（例）
  // Student partialUpdateStudentWithCourses(...);
  // List<Student> searchStudents(...);

  /**
   * 条件に基づいて受講生詳細情報を取得します。
   *
   * @param furigana       ふりがなによる検索（部分一致、null または空文字の場合は無視）
   * @param includeDeleted 削除済みデータを含めるかどうか
   * @param deletedOnly    削除済みデータのみ取得するかどうか
   * @return 受講生詳細情報のリスト
   */
  List<StudentDetailDto> getStudentList(
      String furigana, boolean includeDeleted, boolean deletedOnly);

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
   * 既存の受講生に対して、新しい受講コースを追加します。
   *
   * <p>このメソッドは、すでに存在する {@code student_id + course_name} の組み合わせを 保持したまま、新たなコース情報だけをデータベースに登録します。
   * 既存のコースは削除されず、重複も防止されます。
   *
   * @param studentId  受講生の識別子（UUID文字列表現）
   * @param newCourses 追加対象の新しいコース情報のリスト {@code studentId} に紐づけられている必要があります
   */
  void appendCourses(byte[] studentId, List<StudentCourse> newCourses);

  /**
   * 受講生情報のみを更新します（コース情報は変更しません）。
   *
   * @param student 更新対象の受講生情報
   */
  void updateStudentInfoOnly(Student student);

  /**
   * 受講生IDにより受講生情報を取得します。
   *
   * @param studentId 受講生ID
   * @return 受講生エンティティ
   */
  Student findStudentById(byte[] studentId);

  /**
   * 受講生IDに紐づくコース情報を取得します。
   *
   * @param studentId 受講生ID
   * @return コースエンティティのリスト
   */
  List<StudentCourse> searchCoursesByStudentId(byte[] studentId);

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
  void softDeleteStudent(byte[] studentId);

  /**
   * 論理削除された受講生を復元します。
   *
   * @param studentId 受講生ID
   */
  void restoreStudent(byte[] studentId);

  /**
   * 受講生情報とそのコース情報を物理削除します（管理者専用）。
   *
   * @param studentId 削除対象の受講生ID
   */
  void forceDeleteStudent(byte[] studentId);

  /**
   * 指定した受講生の既存コースを全て削除し、与えられた一覧に置き換えます。
   *
   * <p>処理はトランザクション内で行われ、削除と挿入は原子性を保ちます。<br>
   * 呼び出し側で {@code updateStudentInfoOnly(...)} 等の基本情報更新を別途行ってください。 また、レスポンスに返す際は最終状態を必ず DB
   * から再取得することを推奨します。
   *
   * @param studentIdBytes UUID文字列表現の受講生ID（バイナリ）
   * @param newCourses     置換後に保持するコース一覧（null/空の場合は「全削除のみ」）
   * @throws ResourceNotFoundException 該当受講生が存在しない場合
   * @implNote 各 {@code StudentCourse} の {@code studentId} は本メソッド内で安全のため再セットします。
   * @since 1.0
   */
  void replaceCourses(byte[] studentIdBytes, List<StudentCourse> newCourses);
}
