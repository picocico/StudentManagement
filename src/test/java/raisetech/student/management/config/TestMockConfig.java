package raisetech.student.management.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.service.StudentService;

/**
 * テスト用のモックBean定義クラス。
 */
@TestConfiguration
public class TestMockConfig {

  @Bean
  @Primary
  public StudentService studentService() {
    return Mockito.mock(StudentService.class);
  }

  @Bean
  @Primary
  public StudentConverter studentConverter() {
    return Mockito.mock(StudentConverter.class);
  }
}
