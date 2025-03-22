package raisetech.student.management.dto;

import jakarta.validation.Valid;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;

@Getter
@Setter
public class StudentRegistrationRequest {

  @Valid
  private Student student;  // これが重要：バリデーションの対象となるクラスに @Valid を付ける
  private List<StudentCourse> courses;
  private String studentId;
}



