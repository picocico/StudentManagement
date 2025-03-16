package raisetech.student.management.repository;

import java.util.List;
import org.apache.ibatis.annotations.*;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;

/**
 * 受講生情報を扱うリポジトリ。
 * <p>
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
      @Result(property = "studentId", column = "student_id"),
  })
  List<Student> search();

  @Select("SELECT * FROM student_courses WHERE student_id = #{studentId}")
  List<StudentCourse> findCoursesByStudentId(@Param("studentId") String studentId);

  @Select("SELECT * FROM student_courses")
  List<StudentCourse> findAllCourses();

  /**
   * insertStudent(Student student): 受講生の情報を登録するSQL。
   */

  @Insert(
      "INSERT INTO students (student_id, full_name, furigana, nickname, email, location, age, gender, remarks, created_at, is_deleted) "
          +
          "VALUES (#{studentId}, #{fullName}, #{furigana}, #{nickname}, #{email}, #{location}, #{age}, #{gender}, #{remarks}, #{createdAt}, false)")
  void insertStudent(Student student);

  /**
   * insertCourse(StudentCourse course): 受講コース情報を登録するSQL。
   */

  @Insert(
      "INSERT INTO student_courses (course_id, student_id, course_name, start_date, end_date, created_at) "
          +
          "VALUES (#{courseId}, #{studentId}, #{courseName}, #{startDate}, #{endDate}, #{createdAt})")
  void insertCourse(StudentCourse course);

}
