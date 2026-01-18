package raisetech.student.management.repository;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import raisetech.student.management.data.StudentCourse;

/** 受講生のコース情報に関するデータベース操作を行うMyBatisリポジトリ。 */
@Mapper
public interface StudentCourseRepository {

  /**
   * 受講生の複数コース情報を一括で登録します。
   *
   * @param courses 登録するコースのリスト
   */
  void insertCourses(@Param("list") List<StudentCourse> courses);

  /**
   * 受講生のコース情報（StudentCourse）を更新します。
   *
   * <p>更新対象の行は、引数 {@code course} が保持する識別子（例：courseId/studentId など）
   * を条件として特定し、受講期間や申込ステータス等の各項目を更新する想定です。
   *
   * @param course 更新内容を保持するコース情報（更新条件のキーを含むこと）
   * @return 更新された行数（通常は 0 または 1）
   */
  int updateCourse(StudentCourse course);

  /**
   * 受講情報（StudentCourse）を「指定された項目のみ」部分更新します（Selective Update）。
   *
   * <p>引数 {@code course} に設定されている値のうち、{@code null} ではない項目だけを更新し、 {@code null}
   * の項目はDBの既存値を保持する想定です（MyBatisの動的SQL等で実現）。
   *
   * <p>更新対象の行は、{@code course} が保持する識別子（例：studentId と courseId など）で特定します。
   *
   * @param course 更新条件のキー（studentId/courseId 等）および更新内容（nullでない項目）を保持する受講情報
   * @return 更新された行数（通常は 0 または 1）
   */
  int updateCourseSelective(StudentCourse course);

  /**
   * 存在しない場合に限り、新しいコースを追加します。
   *
   * @param course 追加対象のコース情報
   */
  int insertIfNotExists(StudentCourse course);

  /** 指定された受講生IDに紐づくすべてのコース情報を削除します。 */
  void deleteCoursesByStudentId(@Param("studentId") UUID studentId);

  /**
   * 指定した学生IDに紐づく受講情報のうち、指定したコースID一覧に一致するレコードを削除します。
   *
   * <p>例：学生の受講コースの差分更新（不要になったコースの削除）などで利用します。 {@code courseIds} が空の場合は、SQL側の実装によっては削除対象なし（0件）となります。
   *
   * @param studentId 対象学生のID
   * @param courseIds 削除対象のコースID一覧
   * @return 削除された行数
   */
  int deleteCoursesByCourseIds(
      @Param("studentId") UUID studentId, @Param("courseIds") List<UUID> courseIds);

  /** 指定された受講生IDに紐づくコース情報を取得します。 */
  List<StudentCourse> findCoursesByStudentId(@Param("studentId") UUID studentId);

  /**
   * コース申し込み状況にに基づいて受講生情報を検索します。
   *
   * @param studentIds ふりがなによる部分一致検索条件（nullまたは空文字は無視）
   * @param applicationStatus 論理削除された受講生も含めるかどうか
   * @return 条件に一致する受講生情報のリスト
   */
  List<StudentCourse> findCoursesByStudentIds(
      @Param("studentIds") List<UUID> studentIds,
      @Param("applicationStatus") String applicationStatus);

  /** すべての受講生コース情報を取得します。 */
  List<StudentCourse> findAllCourses();
}
