package raisetech.student.management.repository;

import java.util.List;
import org.apache.ibatis.annotations.*;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;

/**
 * 受講生情報を扱うリポジトリ。
 *
 * 全件検索や単一条件での検索、コース情報の検索が行えるクラスです。
 */
@Mapper
public interface StudentRepository {

  /**
   * 全件検索します。
   *
   * @return 全件検索した受講生情報の一覧
   */

  @Select("SELECT * FROM students")
  @Results({
      //  `@Many` を使うために `@Results` を最低限定義
      //  `courses` に `findCoursesByStudentId()` を関連付け
      @Result(property = "studentId", column = "student_id"),
      @Result(property = "courses", column = "student_id",
          many = @Many(select = "raisetech.student.management.repository.StudentRepository.findCoursesByStudentId"))
  })
  List<Student> search();

  @Select("SELECT * FROM student_courses WHERE student_id = #{studentId}")
  List<StudentCourse> findCoursesByStudentId(@Param("studentId") String studentId);

  @Select("SELECT * FROM student_courses")
  List<StudentCourse> findAllCourses();
}
