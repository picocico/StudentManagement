package raisetech.student.management.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(force = true) // 強制的にデフォルト値をセットする
@AllArgsConstructor
public class StudentCourse {

  private String courseId; // コースID　
  private String studentId; // 生徒ID（外部キー）
  private String courseName; // コース名
  private LocalDate startDate; // 開始日
  private LocalDate endDate; // 終了日（NULL 可能）
  private LocalDateTime createdAt; // 作成日
}

