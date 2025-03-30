package raisetech.student.management.dto;

import jakarta.validation.Valid;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;

@Setter
@Getter
public class StudentRegistrationRequest {

  @Valid // これが重要：バリデーションの対象となるクラスに @Valid を付ける
  private Student student;
  private List<StudentCourse> courses;
  private String studentId;

  private boolean deleted;  // フィールド名を isDeleted ではなく deleted に変更

}



