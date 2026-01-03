package raisetech.student.management.repository;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import raisetech.student.management.data.StudentCourse;

/**
 * 受講生のコース情報に関するデータベース操作を行うMyBatisリポジトリ。
 */
@Mapper
public interface StudentCourseRepository {

  /**
   * 受講生の複数コース情報を一括で登録します。
   *
   * @param courses 登録するコースのリスト
   */
  void insertCourses(@Param("list") List<StudentCourse> courses);

  /**
   * 存在しない場合に限り、新しいコースを追加します。
   *
   * @param course 追加対象のコース情報
   */
  int insertIfNotExists(StudentCourse course);

  /**
   * 指定された受講生IDに紐づくすべてのコース情報を削除します。
   */
  void deleteCoursesByStudentId(@Param("studentId") UUID studentId);

  /**
   * 指定された受講生IDに紐づくコース情報を取得します。
   */
  List<StudentCourse> findCoursesByStudentId(@Param("studentId") UUID studentId);

  /**
   * コース申し込み状況にに基づいて受講生情報を検索します。
   *
   * @param studentIds        ふりがなによる部分一致検索条件（nullまたは空文字は無視）
   * @param applicationStatus 論理削除された受講生も含めるかどうか
   * @return 条件に一致する受講生情報のリスト
   */
  List<StudentCourse> findCoursesByStudentIds(
      @Param("studentIds") List<UUID> studentIds,
      @Param("applicationStatus") String applicationStatus
  );

  /**
   * すべての受講生コース情報を取得します。
   */
  List<StudentCourse> findAllCourses();
}
