package raisetech.student.management.dto;

import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor // ← 引数なしのデフォルトコンストラクタも自動生成
@AllArgsConstructor // ← 全フィールド引数のコンストラクタを自動生成
public class StudentRegistrationRequest {
  @Valid
  private StudentDto student;
  @Valid
  private List<StudentCourseDto> courses;
  private boolean deleted;
}




