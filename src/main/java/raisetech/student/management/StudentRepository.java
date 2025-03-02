package raisetech.student.management;

import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StudentRepository {

  @Select("SELECT * FROM students")
  @Results({
      //  `@Many` を使うために `@Results` を最低限定義
      //  `courses` に `findCoursesByStudentId()` を関連付け
      @Result(property = "studentId", column = "student_id"),
      @Result(property = "courses", column = "student_id",
          many = @Many(select = "raisetech.student.management.StudentRepository.findCoursesByStudentId"))
  })
  List<Student> search();

  @Select("SELECT * FROM student_courses WHERE student_id = #{studentId}")
  List<StudentCourse> findCoursesByStudentId(@Param("studentId") String studentId);

  @Select("SELECT * FROM student_courses")
  List<StudentCourse> findAllCourses();
}
