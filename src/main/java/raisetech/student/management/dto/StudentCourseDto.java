package raisetech.student.management.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseDto {

  private String courseName;
  private LocalDate startDate;
  private LocalDate endDate;
}

