package raisetech.student.management;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * WARファイルとしてデプロイする際の初期化設定クラス。
 * <p>
 * このクラスは、アプリケーションを外部のサーブレットコンテナ（例：Tomcat）に デプロイするために必要な設定を提供します。 通常、Spring Boot アプリを自己実行（JAR）ではなく
 * WAR 形式で配備する場合に使用されます。
 */
public class ServletInitializer extends SpringBootServletInitializer {

  /**
   * アプリケーションの起動設定をカスタマイズします。
   * <p>
   * 外部サーブレットコンテナ上でのデプロイ時に呼び出され、 エントリポイントとなる {@code Application.class} を指定します。
   *
   * @param application SpringApplicationBuilder インスタンス
   * @return カスタマイズされた SpringApplicationBuilder
   */
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(Application.class);
  }
}
