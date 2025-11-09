// src/test/java/.../DebugControllerProfileOffTest.java
package raisetech.student.management.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest // プロファイル指定なし（= test を明示的に有効化しない）
class DebugControllerProfileOffTest {

  @Autowired org.springframework.context.ApplicationContext ctx;

  @Test
  void debugController_isNotLoaded_whenProfileIsNotTest() {
    assertThat(ctx.getBeansOfType(DebugStudentController.class)).isEmpty();
  }
}
