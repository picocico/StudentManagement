package raisetech.student.management.util;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * UUID と byte[16] の相互変換を提供するユーティリティクラス。
 *
 * <p>DB では UUID を BINARY(16)（{@code byte[16]}）として保持し、
 * * アプリケーションや API では UUID 文字列表現（例: {@code 123e4567-e89b-12d3-a456-426614174000}） *
 * を扱うため、その橋渡しとして利用します。
 */
public class UUIDUtil {

  // インスタンス化禁止
  private UUIDUtil() {
    throw new AssertionError("UUIDUtil should not be instantiated.");
  }

  // ========================
  // UUID ⇔ byte[16]
  // ========================

  /**
   * UUID から {@code byte[16]}（バイナリ形式）に変換します。
   *
   * @param uuid UUID オブジェクト（null 不可）
   * @return UUID のバイナリ表現（常に長さ 16 の配列）
   * @throws IllegalArgumentException {@code uuid} が null の場合
   */
  public static byte[] toBytes(UUID uuid) {
    if (uuid == null) {
      throw new IllegalArgumentException("UUIDはnullにできません");
    }
    ByteBuffer bb = ByteBuffer.allocate(16);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  /**
   * {@code byte[16]} から UUID オブジェクトに変換します。
   *
   * @param bytes UUID のバイナリ表現（長さ 16 の配列を期待）
   * @return UUID オブジェクト
   * @throws IllegalArgumentException {@code bytes} が null または長さ 16 以外の場合
   */
  public static UUID fromBytes(byte[] bytes) {
    if (bytes == null || bytes.length != 16) {
      throw new IllegalArgumentException("UUIDの形式が不正です");
    }
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    long mostSigBits = buffer.getLong();
    long leastSigBits = buffer.getLong();
    return new UUID(mostSigBits, leastSigBits);
  }
}
