package raisetech.student.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import raisetech.student.management.config.security.CustomAuthenticationEntryPoint;

/**
 * Spring Securityの設定クラス。
 * <p>
 * 本設定では、物理削除API（/api/admin/**）へのアクセスに認証を要求し、
 * その他のAPIは認証不要としています。また、Basic認証とCSRF無効化を明示的に設定します。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorizeなどのメソッドレベルセキュリティを有効化

public class SecurityConfig {

 /**
  * アプリケーション全体のセキュリティフィルタチェーンを定義します。
  *
  * @param http HttpSecurityオブジェクト（Spring Securityの設定API）
  * @return セキュリティフィルタチェーン
  * @throws Exception セキュリティ設定中に例外が発生した場合
  */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // /api/admin/** へのアクセス（物理削除API）には認証が必要
            .requestMatchers("/api/admin/**").authenticated()
            .anyRequest().permitAll() // 他はすべて許可
        )
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
        )
        .httpBasic(basic -> {}) // 明示的にhttpBasicを有効化（空のラムダで設定）
        .csrf(AbstractHttpConfigurer::disable); // 明示的にCSRF保護を無効化

    return http.build();
  }

  /**
   * 認証に使用するユーザー情報を提供するUserDetailsServiceを定義します。
   * <p>
   * 簡易的にインメモリで"admin"ユーザーを定義しています。
   *
   * @return InMemoryUserDetailsManagerのインスタンス
   */
  @Bean
  public UserDetailsService userDetailsService() {
    // 簡易的にインメモリでユーザーを定義
    return new InMemoryUserDetailsManager(
        User.withUsername("admin")
            .password("{noop}password") // {noop}はパスワードエンコーディング無し
            .roles("ADMIN")
            .build()
    );
  }
}
