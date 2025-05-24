package raisetech.student.management.repository;

import java.util.List;
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
  void insertIfNotExists(StudentCourse course);

  /**
   * 指定された受講生IDに紐づくすべてのコース情報を削除します。
   */
  void deleteCoursesByStudentId(@Param("studentId") byte[] studentId);

  /**
   * 指定された受講生IDに紐づくコース情報を取得します。
   */
  List<StudentCourse> findCoursesByStudentId(@Param("studentId") byte[] studentId);

  /**
   * すべての受講生コース情報を取得します。
   */
  List<StudentCourse> findAllCourses();
}


