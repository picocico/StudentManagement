package raisetech.student.management.dto;

import java.util.List;
import lombok.Getter;
import lombok.AllArgsConstructor;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;

@Getter
@AllArgsConstructor
public class StudentWithCoursesDTO {

  private Student student;
  private List<StudentCourse> courses;
}

