package raisetech.student.management.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.service.StudentService;

/**
 * {@code @TestConfiguration} for {@code @WebMvcTest} - Controller単体の HTTP
 * 挙動検証を安定化するため、Service/Converter を {@code @Primary} Mock で差替える。 - Jackson/MessageConverters
 * は{@code @WebMvcTest} 標準の自動構成に任せ、差分を生まない。 - 本番と異なる Mapper/Converter を作らないよう注意（ここでは作らない）。
 */
@TestConfiguration
public class TestMockConfig {

  // @Primary: @WebMvcTest が作るコンテキストに、
  // 実装クラスの本物Beanが紛れないようモックを最優先に差し替えるため
  @Bean
  @Primary
  public StudentService service() {
    return Mockito.mock(StudentService.class);
  }

  @Bean
  @Primary
  public StudentConverter converter() {
    return Mockito.mock(StudentConverter.class);
  }

}
