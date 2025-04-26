package raisetech.student.management.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailDto {
  private StudentDto student;
  private List<StudentCourseDto> courses;
}

