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

  /**
   * selectStudent:特定の受講生情報をstudentIdで探し出すSQL
   */

  @Select("SELECT * FROM students WHERE student_id = #{studentId}")
  Student findStudentById(@Param("studentId") String studentId);

  /**
   * selectStudent:特定の受講生情報をfuriganaで探し出すSQL 全角スペースを半角スペースに変換して検索
   */

  @Select("SELECT * FROM students WHERE REPLACE(furigana, '　', ' ') LIKE CONCAT('%', REPLACE(#{furigana}, '　', ' '), '%')")
  List<Student> findStudentsByFurigana(@Param("furigana") String furigana);

  /**
   * selectStudent: 論理削除されていない受講生の情報を検索するSQL。
   */

  @Select("SELECT * FROM students WHERE is_deleted = false")
  List<Student> searchActiveStudents();


  /**
   * updateStudent:特定の受講生情報を更新するSQL
   */

  @Update(
      "UPDATE students SET full_name = #{fullName}, furigana = #{furigana}, nickname = #{nickname}, "
          +
          "email = #{email}, location = #{location}, age = #{age}, gender = #{gender}, remarks = #{remarks}, "
          +
          "is_deleted = #{deleted} WHERE student_id = #{studentId}"
  )
  void updateStudent(Student student);


  /**
   * deleteStudent:受講生情報をstudentIdで特定し削除するSQL
   */

  @Delete("DELETE FROM student_courses WHERE student_id = #{studentId}")
  void deleteCoursesByStudentId(@Param("studentId") String studentId);
}
