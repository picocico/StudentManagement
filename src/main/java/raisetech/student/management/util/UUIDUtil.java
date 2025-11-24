package raisetech.student.management.util;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * UUIDとBase64、byte[]相互の変換を提供するユーティリティクラス。
 *
 * <p>UUID（128bit）をバイナリ配列（byte[16]）やURLセーフなBase64形式に変換し、 データベース（BINARY(16)）やWeb
 * API（Base64文字列）との整合性を保つために使用されます。
 */
public class UUIDUtil {

  // インスタンス化禁止
  private UUIDUtil() {
    throw new AssertionError("UUIDUtil should not be instantiated.");
  }

  /**
   * UUIDからbyte[16]（バイナリ形式）に変換します。
   *
   * @param uuid UUIDオブジェクト
   * @return UUIDのバイナリ表現（byte[16]）
   */
  /**
   * UUID -> 16バイト配列
   */
  public static byte[] fromUUID(UUID uuid) {
    if (uuid == null) {
      throw new IllegalArgumentException("UUIDはnullにできません");
    }
    ByteBuffer bb = ByteBuffer.allocate(16);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  /**
   * byte[16]からUUIDオブジェクトに変換します。
   *
   * @param bytes UUIDのバイナリ表現（byte[16]）
   * @return UUIDオブジェクト
   * @throws IllegalArgumentException 長さが16バイトでない場合
   */
  public static UUID toUUID(byte[] bytes) {
    if (bytes == null || bytes.length != 16) {
      throw new IllegalArgumentException("UUIDの形式が不正です");
    }
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    long mostSigBits = buffer.getLong();
    long leastSigBits = buffer.getLong();
    return new UUID(mostSigBits, leastSigBits);
  }
}
