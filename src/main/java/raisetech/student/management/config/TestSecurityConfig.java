package raisetech.student.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * テスト（"test" プロファイル）実行時専用のSpring Security設定クラス。
 *
 * <p>この設定は、特定のデバッグ用エンドポイントへのアクセスを、
 * メインのセキュリティ設定（認証・認可）よりも優先して許可するために使用されます。
 *
 * <p><b>【 @WebMvcTest における意図的なセキュリティ無効化 】</b>
 * {@code @WebMvcTest} (コントローラー層の単体テスト) を実行する際、 認証・認可のロジックがテストの妨げになることがあります（例：401/403の発生）。
 *
 * <p>{@code @Order(0)} と {@code securityMatcher("/**")} を指定することで、
 * メインの {@code SecurityConfig} よりも先にこの設定が適用され、"test" プロファイルが
 * 有効な場合は<b>すべてのエンドポイントに対する認証・認可を無効化</b>します。
 *
 * <p>これにより、{@link raisetech.student.management.controller.StudentController} や
 * {@link raisetech.student.management.controller.admin.AdminStudentController} の
 * 単体テストが、セキュリティ設定を意識することなく（モック化することなく） 実行できることを意図しています。
 */
@Profile("test")
@Configuration
public class TestSecurityConfig {

  /**
   * デバッグ用エンドポイント（/api/students/debug-dto, /api/students/debug-raw）専用の セキュリティフィルタチェーンを定義します。
   *
   * <p>{@code @Order(0)} を指定することで、他の（デフォルトの）{@link SecurityFilterChain} よりも <b>最優先</b>で評価されます。
   *
   * <p>{@code securityMatcher} で指定された2つのパスにのみこの設定が適用され、 CSRF保護が無効化された上で、すべてのアクセス（認証なし）が許可されます。
   *
   * @param http {@link HttpSecurity} ビルダ
   * @return デバッグエンドポイント専用の {@link SecurityFilterChain}
   * @throws Exception HttpSecurity設定時の例外
   */
  @Order(0)
  @Bean(name = "testSecurityFilterChain")
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        // ★ 全てのリクエストに適用（securityMatcher を広げる or 省略）
        .securityMatcher("/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .httpBasic(b -> b.disable())
        .formLogin(f -> f.disable())
        .build();
  }
}
