package raisetech.student.management.repository;

import java.util.List;
import org.apache.ibatis.annotations.*;
import raisetech.student.management.data.StudentCourse;

/**
 * 学生のコース情報に関するデータベース操作を行うMyBatisリポジトリ。
 */
@Mapper
public interface StudentCourseRepository {

  /**
   * 学生のコース情報を登録します。
   *
   * @param course 登録するコース情報
   */
  @Insert("""
      INSERT INTO student_courses (course_id, student_id, course_name, start_date, end_date, created_at)
      VALUES (UUID(), #{studentId}, #{courseName}, #{startDate}, #{endDate}, now())
      """)
  void insertCourse(StudentCourse course);

  /**
   * 指定された学生IDに紐づくすべてのコース情報を削除します。
   *
   * @param studentId 対象の学生ID
   */
  @Delete("DELETE FROM student_courses WHERE student_id = #{studentId}")
  void deleteCoursesByStudentId(String studentId);

  /**
   * 指定された学生IDに紐づくコース情報を取得します。
   *
   * @param studentId 学生ID
   * @return 該当するコース情報のリスト
   */
  @Select("SELECT * FROM student_courses WHERE student_id = #{studentId}")
  List<StudentCourse> findCoursesByStudentId(String studentId);

  /**
   * すべての学生コース情報を取得します。
   *
   * @return 全コース情報のリスト
   */
  @Select("SELECT * FROM student_courses")
  List<StudentCourse> findAllCourses();
}


