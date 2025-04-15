package raisetech.student.management.dto;

import jakarta.validation.Valid;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;

@Data
@AllArgsConstructor // ← 全フィールド引数のコンストラクタを自動生成
@NoArgsConstructor  // ← 引数なしのデフォルトコンストラクタも自動生成
@Setter
@Getter
public class StudentRegistrationRequest {

  @Valid // これが重要：バリデーションの対象となるクラスに @Valid を付ける
  private Student student;
  private List<StudentCourse> courses;
  private boolean deleted;  // フィールド名を isDeleted ではなく deleted に変更

}



