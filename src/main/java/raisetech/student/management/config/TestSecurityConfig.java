package raisetech.student.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Profile("test")
@Configuration
public class TestSecurityConfig {

  // デバッグ用エンドポイント専用のチェーン（最優先で評価）
  @Order(0)
  @Bean(name = "testSecurityFilterChain")
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .securityMatcher("/api/students/debug-dto", "/api/students/debug-raw") // この2パスだけ
        .csrf(csrf -> csrf.disable())          // CSRF 無効（POSTしやすく）
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()
        )
        .build();
  }
}
