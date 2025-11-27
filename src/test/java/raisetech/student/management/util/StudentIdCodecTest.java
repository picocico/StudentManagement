package raisetech.student.management.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import raisetech.student.management.exception.InvalidIdFormatException;

/**
 * {@link StudentIdCodec} の単体テストクラス。
 *
 * <p>UUID 文字列表現と BINARY(16)（UUID 生バイト）の相互変換ロジックについて、
 * 正常系／異常系の振る舞いを網羅的に検証します。
 */
public class StudentIdCodecTest {

  /**
   * テスト対象となる {@link StudentIdCodec} の実インスタンス。
   *
   * <p>本テストではモックを利用せず、実装クラスの純粋な振る舞いを検証します。
   */
  private final StudentIdCodec codec = new StudentIdCodec();

  /**
   * テスト共通で使う固定 UUID。
   *
   * <p>UUID 自体の値はテストの関心事ではなく、再現性のある固定値であればよい前提です。
   */
  private final UUID FIXED_UUID = UUID.fromString("12345678-9abc-def0-1234-56789abcdef0");

  /**
   * UUID を 16 バイトの配列（BINARY(16)）に変換するテスト用ユーティリティです。
   *
   * <p>本番コード側でも、UUID を {@code BINARY(16)} に変換するユーティリティ
   * （例: {@code UUIDUtil#toBytes(UUID)} 等）が同じ変換を担うことを想定しています。
   *
   * @param uuid 変換対象の UUID
   * @return 引数の UUID を表現する 16 バイト配列
   */
  private static byte[] toBytes(UUID uuid) {
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  /**
   * FIXED_UUID を BINARY(16) に変換したもの（UUID の生 16 バイト表現）。
   *
   * <p>encode／decode の結果検証に利用します。
   */
  private final byte[] FIXED_UUID_BYTES = toBytes(FIXED_UUID);

  /**
   * FIXED_UUID の標準的な UUID 文字列表現。
   *
   * <p>encode／decode の期待値として利用します。
   */
  private final String FIXED_UUID_STRING = FIXED_UUID.toString();

  // ------------------------------------------------------------
  // encodeId のテスト
  // ------------------------------------------------------------

  /**
   * {@link StudentIdCodec#encodeId(byte[])} の振る舞いを検証するテストグループ。
   */
  @Nested
  class EncodeIdTest {

    /**
     * 正常系: 16 バイトの UUID バイト配列を渡した場合に、 対応する UUID 文字列表現が返されることを検証します。
     */
    @Test
    void encodeId_正常系_16バイトのUUIDバイト配列を正しくUUID化できること() {
      String result = codec.encodeId(FIXED_UUID_BYTES);

      assertThat(result).isEqualTo(FIXED_UUID_STRING);
    }

    /**
     * 正常系: 引数に {@code null} を渡した場合に、結果も {@code null} が返されることを検証します。
     *
     * <p>「null はそのまま null を返す」という仕様の確認です。
     */
    @Test
    void encodeId_正常系_nullの場合はnullを返すこと() {
      String result = codec.encodeId(null);

      assertThat(result).isNull();
    }

    /**
     * 異常系: 16 バイト以外の長さの配列を渡した場合に、 {@link IllegalArgumentException} がスローされることを検証します。
     *
     * <p>UUID/BINARY(16) の前提を満たさない入力が拒否されることを確認します。
     */
    @Test
    void encodeId_異常系_16バイト以外の長さの場合はIllegalArgumentExceptionがスローされること() {
      byte[] invalid = new byte[]{0x01, 0x02, 0x03, 0x04}; // 4バイト

      assertThatThrownBy(() -> codec.encodeId(invalid))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("IDの形式が不正です");
    }
  }

  // ------------------------------------------------------------
  // decodeUuidBytesOrThrow のテスト（UUID文字列 → byte[16]）
  // ------------------------------------------------------------

  /**
   * {@link StudentIdCodec#decodeUuidBytesOrThrow(String)} の振る舞いを検証する テストグループ（UUID 文字列 →
   * {@code byte[16]}）。
   */
  @Nested
  class DecodeTest {

    /**
     * 正常系: 正しい UUID 文字列表現を渡した場合に、 長さ 16 のバイト配列へ復元されることを検証します。
     *
     * <p>さらに、復元したバイト配列を UUID に戻したときに元の文字列表現と一致することも確認し、
     * 往復変換の整合性を保証します。
     */
    @Test
    void decodeUuidBytesOrThrow_正常系_UUID文字列を正しく16バイト配列にデコードできること() {
      String uuidString = "123e4567-e89b-12d3-a456-426614174000";
      byte[] result = codec.decodeUuidBytesOrThrow(uuidString);
      assertThat(result).hasSize(16);

      // 往復させて検証
      UUID actual = UUIDUtil.fromBytes(result);
      assertThat(actual.toString()).isEqualTo(uuidString);
    }

    /**
     * 異常系: UUID として不正な文字列を渡した場合に、 {@link InvalidIdFormatException} がスローされることを検証します。
     *
     * <p>UUID 形式チェックに失敗したケースのハンドリング確認です。
     */
    @Test
    void decodeUuidBytesOrThrow_異常系_UUIDとして不正な文字列の場合はInvalidIdFormatExceptionがスローされること() {
      String invalid = "%%%invalid-uuid%%%";

      assertThatThrownBy(() -> codec.decodeUuidBytesOrThrow(invalid))
          .isInstanceOf(InvalidIdFormatException.class)
          .hasMessageContaining("IDの形式が不正です（UUID）");
    }

    /**
     * 異常系: 引数が {@code null} の場合に {@link InvalidIdFormatException} が スローされることを検証します。
     *
     * <p>{@code null} は許容されない前提であることをテストで明示します。
     */
    @Test
    void decodeUuidBytesOrThrow_異常系_nullの場合はInvalidIdFormatExceptionがスローされること() {
      assertThatThrownBy(() -> codec.decodeUuidBytesOrThrow(null))
          .isInstanceOf(InvalidIdFormatException.class);
    }
  }

  // ------------------------------------------------------------
  // generateNewIdBytes のテスト
  // ------------------------------------------------------------

  /**
   * {@link StudentIdCodec#generateNewIdBytes()} の振る舞いを検証するテストグループ。
   *
   * <p>「必ず 16 バイト長で返ってくる」という契約を確認します。
   */
  @Nested
  class GenerateNewIdBytesTest {

    /**
     * 正常系: 新規採番された ID バイト配列が {@code null} ではなく、 かつ長さ 16 バイトであることを検証します。
     *
     * <p>乱数の中身そのものには立ち入らず、形式上の契約のみを確認します。
     */
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

  /**
   * {@link StudentIdCodec#decodeUuidOrThrow(String)} の振る舞いを検証するテストグループ。
   *
   * <p>UUID 文字列 → {@link UUID} オブジェクトへの変換に関する正常系／異常系を確認します。
   */
  @Nested
  class DecodeUuidOrThrowTest {

    /**
     * 正常系: 正しい UUID 文字列表現を渡した場合に、 元の {@link UUID} オブジェクトとして復元できることを検証します。
     */
    @Test
    void decodeUuidOrThrow_正常系_UUID文字列から元のUUIDに復元できること() {
      UUID result = codec.decodeUuidOrThrow(FIXED_UUID_STRING);

      assertThat(result).isEqualTo(FIXED_UUID);
    }

    /**
     * 異常系: UUID として不正な文字列を渡した場合に、 {@link InvalidIdFormatException} がスローされることを検証します。
     *
     * <p>UUID 形式チェックに失敗した際のエラーハンドリング確認です。
     */
    @Test
    void decodeUuidOrThrow_異常系_UUIDとして不正な場合はInvalidIdFormatExceptionがスローされること() {
      String invalid = "###invalid###";

      assertThatThrownBy(() -> codec.decodeUuidOrThrow(invalid))
          .isInstanceOf(InvalidIdFormatException.class)
          .hasMessageContaining("IDの形式が不正です（UUID）");
    }

    /**
     * 異常系: 引数が {@code null} の場合に、 「null は許容されない」という前提で {@link InvalidIdFormatException} が
     * スローされることを検証します。
     */
    @Test
    void decodeUuidOrThrow_異常系_nullの場合はInvalidIdFormatExceptionがスローされること() {
      assertThatThrownBy(() -> codec.decodeUuidOrThrow(null))
          .isInstanceOf(InvalidIdFormatException.class)
          .hasMessageContaining("IDはnullにできません（UUID）");
    }
  }

  // ------------------------------------------------------------
  // decodeUuidBytesOrThrow のテスト
  // ------------------------------------------------------------

  /**
   * {@link StudentIdCodec#decodeUuidBytesOrThrow(String)} について、 固定 UUID
   * を用いて「往復変換が成り立つこと」をより直接的に検証するテストグループです。
   */
  @Nested
  class DecodeUuidBytesOrThrowTest {

    /**
     * 正常系: 固定 UUID 文字列から 16 バイト配列に復元した結果が、 あらかじめ用意した {@link #FIXED_UUID_BYTES} と完全一致することを検証します。
     */
    @Test
    void decodeUuidBytesOrThrow_正常系_UUID文字列からUUIDバイト配列に復元できること() {
      byte[] result = codec.decodeUuidBytesOrThrow(FIXED_UUID_STRING);

      assertThat(result).containsExactly(FIXED_UUID_BYTES);
    }

    /**
     * 異常系: UUID として不正な文字列を渡した場合に、 {@link InvalidIdFormatException} がスローされることを検証します。
     *
     * <p>簡易な不正入力パターンに対する防御が機能していることを確認します。
     */
    @Test
    void decodeUuidBytesOrThrow_異常系_UUIDとして不正な場合はInvalidIdFormatExceptionがスローされること() {
      String invalid = "NG!!";

      assertThatThrownBy(() -> codec.decodeUuidBytesOrThrow(invalid))
          .isInstanceOf(InvalidIdFormatException.class)
          .hasMessageContaining("IDの形式が不正です（UUID）");
    }
  }
}
