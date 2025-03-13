package raisetech.student.management.domain;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;

@Getter
@Setter
public class StudentDetail {

  private Student student;
  private List<StudentCourse> studentCourse;

  public StudentDetail(Student student, List<StudentCourse> studentCourse) {
    this.student = student;
    this.studentCourse = studentCourse;
  }

  public StudentDetail() {

  }
}
