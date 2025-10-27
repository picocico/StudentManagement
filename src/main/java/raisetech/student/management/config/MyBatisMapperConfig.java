package raisetech.student.management.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test") // ← テストでは無効
@MapperScan("raisetech.student.management.repository")
public class MyBatisMapperConfig {

}
