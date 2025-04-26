package raisetech.student.management.data;

import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
  private String studentId;
  private String fullName;
  private String furigana;
  private String nickname;
  private String email;
  private String location;
  private Integer age;
  private String gender;
  private String remarks;
  private LocalDateTime createdAt;
  private LocalDateTime deletedAt;
  private Boolean deleted;

  public void softDelete() {
    this.deleted = true;
    this.deletedAt = LocalDateTime.now();
  }

  public void restore() {
    this.deleted = false;
    this.deletedAt = null;
  }
}





