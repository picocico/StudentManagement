package raisetech.student.management.repository;

import java.util.List;
import org.apache.ibatis.annotations.*;
import raisetech.student.management.data.StudentCourse;

/**
 * 受講生のコース情報に関するデータベース操作を行うMyBatisリポジトリ。
 */
@Mapper
public interface StudentCourseRepository {

  /**
   * 受講生のコース情報を登録します。
   *
   * @param course 登録するコース情報
   */
  @Insert("""
      INSERT INTO student_courses (course_id, student_id, course_name, start_date, end_date, created_at)
      VALUES (UUID(), #{studentId}, #{courseName}, #{startDate}, #{endDate}, now())
      """)
  void insertCourse(StudentCourse course);

  /**
   * 指定された受講生IDに紐づくすべてのコース情報を削除します。
   *
   * @param studentId 対象の受講生ID
   */
  @Delete("DELETE FROM student_courses WHERE student_id = #{studentId}")
  void deleteCoursesByStudentId(String studentId);

  /**
   * 指定された受講生IDに紐づくコース情報を取得します。
   *
   * @param studentId 受講生ID
   * @return 該当するコース情報のリスト
   */
  @Select("SELECT * FROM student_courses WHERE student_id = #{studentId}")
  List<StudentCourse> findCoursesByStudentId(String studentId);

  /**
   * すべての受講生コース情報を取得します。
   *
   * @return 全コース情報のリスト
   */
  @Select("SELECT * FROM student_courses")
  List<StudentCourse> findAllCourses();
}


