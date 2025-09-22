package raisetech.student.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.mockito.Mockito;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.service.StudentService;

@TestConfiguration
public class TestMockConfig {

  @Bean @Primary
  public StudentService service() {
    return Mockito.mock(StudentService.class);
  }

  @Bean @Primary
  public StudentConverter converter() {
    return Mockito.mock(StudentConverter.class);
  }

  // ★ Jackson の HTTP メッセージコンバータを“明示的に唯一”にする
  @Bean @Primary
  public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper om) {
    return new MappingJackson2HttpMessageConverter(om);
  }

  // ★ Spring MVC に「このコンバータだけ使え」と渡す
  @Bean @Primary
  public HttpMessageConverters httpMessageConverters(MappingJackson2HttpMessageConverter jackson) {
    return new HttpMessageConverters(jackson); // ← これで Gson 等を事実上排除
  }
}
