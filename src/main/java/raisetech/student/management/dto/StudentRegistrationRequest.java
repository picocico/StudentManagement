package raisetech.student.management.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;

@Getter
@Setter
public class StudentRegistrationRequest {

  private Student student;
  private List<StudentCourse> courses;
}
