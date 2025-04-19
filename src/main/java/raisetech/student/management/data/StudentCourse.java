package raisetech.student.management.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourse {
  private String courseId;
  private String studentId;
  private String courseName;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDateTime createdAt;
}


