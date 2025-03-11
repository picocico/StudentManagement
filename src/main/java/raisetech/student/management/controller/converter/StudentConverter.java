package raisetech.student.management.controller.converter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.domain.StudentDetail;

@Component
public class StudentConverter {

  public List<StudentDetail> convertStudentDetails(List<Student> students,
      List<StudentCourse> courses) {
    Map<String, List<StudentCourse>> studentCourseMap = courses.stream()
        .collect(Collectors.groupingBy(StudentCourse::getStudentId));

    return students.stream()
        .map(student -> new StudentDetail(student,
            studentCourseMap.getOrDefault(student.getStudentId(), List.of())))
        .collect(Collectors.toList());
  }
}

