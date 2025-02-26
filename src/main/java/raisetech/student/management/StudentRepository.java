package raisetech.student.management;

import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StudentRepository {

  @Select("SELECT * FROM students")
  @Results({
      @Result(property = "studentId", column = "student_id"),
      @Result(property = "fullName", column = "full_name"),
      @Result(property = "furigana", column = "furigana"),
      @Result(property = "nickname", column = "nickname"),
      @Result(property = "email", column = "email"),
      @Result(property = "location", column = "location"),
      @Result(property = "age", column = "age"),
      @Result(property = "gender", column = "gender"),
      @Result(property = "createdAt", column = "created_at"),
      @Result(property = "courses", column = "student_id",
          many = @Many(select = "raisetech.student.management.StudentRepository.findCoursesByStudentId"))
  })
  List<Student> search();

  @Select("SELECT * FROM student_courses WHERE student_id = #{studentId}")
  @Results({
      @Result(property = "courseId", column = "course_id"),
      @Result(property = "studentId", column = "student_id"),
      @Result(property = "courseName", column = "course_name"),
      @Result(property = "startDate", column = "start_date"),
      @Result(property = "endDate", column = "end_date"),
      @Result(property = "createdAt", column = "created_at")
  })
  List<StudentCourse> findCoursesByStudentId(@Param("studentId") String studentId);

  //  `@Results` を追加
  @Select("SELECT * FROM student_courses")
  @Results({
      @Result(property = "courseId", column = "course_id"),
      @Result(property = "studentId", column = "student_id"),
      @Result(property = "courseName", column = "course_name"),
      @Result(property = "startDate", column = "start_date"),
      @Result(property = "endDate", column = "end_date"),
      @Result(property = "createdAt", column = "created_at")
  })
  List<StudentCourse> findAllCourses();
}



