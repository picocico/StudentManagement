package raisetech.student.management.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link UUIDUtil} のユニットテスト。
 *
 * <p>UUID と byte[16] の相互変換ロジックについて、
 * 正常系／異常系をシンプルに検証します。
 */
public class UUIDUtilTest {

  // テスト共通で使う固定 UUID（再現性のために固定値を1つ用意）
  private static final UUID FIXED_UUID =
      java.util.UUID.fromString("12345678-9abc-def0-1234-56789abcdef0");

  @Nested
  class FromUUIDTest {

    @Test
    void fromUUID_正常系_16バイト配列に変換できること() {
      byte[] bytes = UUIDUtil.fromUUID(FIXED_UUID);

      // 16バイトであること
      assertThat(bytes)
          .isNotNull()
          .hasSize(16);

      // 往復させると同じ UUID に戻ること
      UUID restored = UUIDUtil.toUUID(bytes);
      assertThat(restored).isEqualTo(FIXED_UUID);
    }
  }

  @Test
  void fromUUID_異常系_nullを渡すとIllegalArgumentExceptionがスローされること() {
    assertThatThrownBy(() -> UUIDUtil.fromUUID(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UUIDはnullにできません");
  }

  @Nested
  class ToUUIDTest {

    @Test
    void toUUID_正常系_16バイト配列からUUIDに復元できること() {
      // まず fromUUID で 16バイト配列を作る
      byte[] bytes = UUIDUtil.fromUUID(FIXED_UUID);

      UUID result = UUIDUtil.toUUID(bytes);

      assertThat(result).isEqualTo(FIXED_UUID);
    }

    @Test
    void toUUID_異常系_nullを渡すとIllegalArgumentExceptionがスローされること() {
      assertThatThrownBy(() -> UUIDUtil.toUUID(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("UUIDの形式が不正です");
    }

    @Test
    void toUUID_異常系_16バイト以外の配列を渡すとIllegalArgumentExceptionがスローされること() {
      byte[] invalid = new byte[4];

      assertThatThrownBy(() -> UUIDUtil.toUUID(invalid))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("UUIDの形式が不正です");
    }
  }
}

