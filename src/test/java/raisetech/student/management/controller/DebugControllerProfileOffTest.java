package raisetech.student.management.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@link DebugStudentController} の {@code @Profile("test")} 設定を検証するテストクラス。
 *
 * <p>Spring Boot を通常のテストプロファイル（test）なしで起動した場合に、
 * {@link DebugStudentController} がコンテナにロードされないことを確認します。
 *
 * <p>このテストが通っていれば、本番起動時にデバッグ用エンドポイントが
 * 誤って有効化されないことの確認材料になります。
 */
@SpringBootTest // プロファイル指定なし（= test を明示的に有効化しない）
class DebugControllerProfileOffTest {

  /**
   * 実際に起動した Spring アプリケーションコンテキスト。
   *
   * <p>このコンテキストから {@link DebugStudentController} の Bean 定義有無を確認します。
   */
  @Autowired
  org.springframework.context.ApplicationContext ctx;

  /**
   * プロファイルが {@code "test"} でない場合、 {@link DebugStudentController} がコンテナに登録されていないことを検証します。
   *
   * <p>{@link org.springframework.context.ApplicationContext#getBeansOfType(Class)} の結果が
   * 空であることをアサートします。
   */
  @Test
  void debugController_isNotLoaded_whenProfileIsNotTest() {
    assertThat(ctx.getBeansOfType(DebugStudentController.class)).isEmpty();
  }
}
