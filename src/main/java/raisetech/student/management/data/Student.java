package raisetech.student.management.data;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Student {

  private String studentId;
  private String fullName;
  private String furigana;
  private String nickname;
  private String email;
  private String location;
  private int age;
  private String gender;
  private String remarks;
  private boolean isDeleted;
  private LocalDateTime createdAt;

  // student_courses のリレーション（One-to-Many）
  private List<StudentCourse> courses;
}


