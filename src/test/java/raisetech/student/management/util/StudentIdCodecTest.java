package raisetech.student.management.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class StudentIdCodecTest {

  private final StudentIdCodec codec = new StudentIdCodec();

  // テスト共通で使う固定UUIDとその派生値
  private final UUID FIXED_UUID = UUID.fromString("12345678-9abc-def0-1234-56789abcdef0");

  /**
   * UUID を 16 バイトの配列（BINARY(16)）に変換するテスト用ユーティリティです。
   *
   * <p>本番コード側でも、UUID を {@code BINARY(16)} に変換するユーティリティ
   * （例: {@code IdCodec} や同等のコンポーネント）が同じ変換を担うことを想定しています。
   */
  private static byte[] toBytes(UUID uuid) {
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  /**
   * FIXED_UUID を BINARY(16) に変換したもの（UUIDの生16バイト表現）
   */
  private final byte[] FIXED_UUID_BYTES = toBytes(FIXED_UUID);

  // URL-safe & no padding の Base64 文字列
  private final String FIXED_BASE64 =
      Base64.getUrlEncoder().withoutPadding().encodeToString(FIXED_UUID_BYTES);

  // ------------------------------------------------------------
  // encodeId のテスト
  // ------------------------------------------------------------
  @Nested
  class EncodeIdTest {

    @Test
    void encodeId_正常系_16バイトのUUIDバイト配列を正しくBase64化できること() {
      String result = codec.encodeId(FIXED_UUID_BYTES);

      assertThat(result).isEqualTo(FIXED_BASE64);
    }

    @Test
    void encodeId_正常系_nullの場合はnullを返すこと() {
      String result = codec.encodeId(null);

      assertThat(result).isNull();
    }

    @Test
    void encodeId_異常系_16バイト以外の長さの場合はIllegalArgumentExceptionがスローされること() {
      byte[] invalid = new byte[]{0x01, 0x02, 0x03, 0x04}; // 4バイト

      assertThatThrownBy(() -> codec.encodeId(invalid))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("UUIDの形式");
    }
  }

  // ------------------------------------------------------------
  // decode(String) のテスト
  // ------------------------------------------------------------
  @Nested
  class DecodeTest {

    @Test
    void decode_正常系_Base64文字列を正しくバイト配列にデコードできること() {
      byte[] result = codec.decode(FIXED_BASE64);

      assertThat(result).containsExactly(FIXED_UUID_BYTES);
    }

    @Test
    void decode_異常系_Base64として不正な文字列の場合はIllegalArgumentExceptionがスローされること() {
      String invalid = "%%%invalid-base64%%%";

      assertThatThrownBy(() -> codec.decode(invalid))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("(Base64)");
    }

    @Test
    void decode_正常系_nullの場合はnullを返すこと() {
      byte[] result = codec.decode(null);

      assertThat(result).isNull();
    }
  }

  // ------------------------------------------------------------
  // generateNewIdBytes のテスト
  // ------------------------------------------------------------
  @Nested
  class GenerateNewIdBytesTest {

    @Test
    void generateNewIdBytes_正常系_16バイト長の配列が返されること() {
      byte[] idBytes = codec.generateNewIdBytes();

      assertThat(idBytes).isNotNull();
      assertThat(idBytes).hasSize(16);
    }
  }

  // ------------------------------------------------------------
  // decodeUuidOrThrow のテスト
  // ------------------------------------------------------------
  @Nested
  class DecodeUuidOrThrowTest {

    @Test
    void decodeUuidOrThrow_正常系_Base64文字列から元のUUIDに復元できること() {
      UUID result = codec.decodeUuidOrThrow(FIXED_BASE64);

      assertThat(result).isEqualTo(FIXED_UUID);
    }

    @Test
    void decodeUuidOrThrow_異常系_Base64として不正な場合はIllegalArgumentExceptionがスローされること() {
      String invalid = "###invalid###";

      assertThatThrownBy(() -> codec.decodeUuidOrThrow(invalid))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("(Base64)");
    }

    @Test
    void decodeUuidOrThrow_異常系_デコード後の長さが16バイトでない場合もIllegalArgumentExceptionがスローされること() {
      // 8バイトのデータをBase64化して渡す（あえて16バイトではない）
      byte[] eightBytes = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
      String base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(eightBytes);

      assertThatThrownBy(() -> codec.decodeUuidOrThrow(base64))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("(Base64)");
      // 内部的には「長さが16バイトではありません」などの例外を catch して
      // 「studentId(Base64)が不正です」のようなメッセージにラップされる想定。
      // テストでは "(Base64)" を含んでいることだけを検証する。
    }
  }

  // ------------------------------------------------------------
  // decodeUuidBytesOrThrow のテスト
  // ------------------------------------------------------------
  @Nested
  class DecodeUuidBytesOrThrowTest {

    @Test
    void decodeUuidBytesOrThrow_正常系_Base64文字列からUUIDバイト配列に復元できること() {
      byte[] result = codec.decodeUuidBytesOrThrow(FIXED_BASE64);

      assertThat(result).containsExactly(FIXED_UUID_BYTES);
    }

    @Test
    void decodeUuidBytesOrThrow_異常系_Base64として不正な場合はIllegalArgumentExceptionがスローされること() {
      String invalid = "NG!!";

      assertThatThrownBy(() -> codec.decodeUuidBytesOrThrow(invalid))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("(Base64)");
    }

    @Test
    void decodeUuidBytesOrThrow_異常系_デコード後の長さが16バイトでない場合もIllegalArgumentExceptionがスローされること() {
      // 8バイトのデータを Base64 化して渡す（あえて 16 バイトではない）
      byte[] eightBytes = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
      String base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(eightBytes);

      assertThatThrownBy(() -> codec.decodeUuidBytesOrThrow(base64))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("(Base64)");
    }
  }
}
