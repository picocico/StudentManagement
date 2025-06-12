package raisetech.student.management.util;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/**
 * UUIDとBase64、byte[]相互の変換を提供するユーティリティクラス。
 * <p>
 * UUID（128bit）をバイナリ配列（byte[16]）やURLセーフなBase64形式に変換し、
 * データベース（BINARY(16)）やWeb API（Base64文字列）との整合性を保つために使用されます。
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
  public static byte[] fromUUID(UUID uuid) {
    ByteBuffer buffer = ByteBuffer.allocate(16);
    buffer.putLong(uuid.getMostSignificantBits());
    buffer.putLong(uuid.getLeastSignificantBits());
    return buffer.array();
  }

  /**
   * byte[16]からUUIDオブジェクトに変換します。
   *
   * @param bytes UUIDのバイナリ表現（byte[16]）
   * @return UUIDオブジェクト
   * @throws IllegalArgumentException 長さが16バイトでない場合
   */
  public static UUID toUUID(byte[] bytes) {
    if (bytes.length != 16) {
      throw new IllegalArgumentException("UUIDバイナリは16バイトである必要があります。");
    }
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    long mostSigBits = buffer.getLong();
    long leastSigBits = buffer.getLong();
    return new UUID(mostSigBits, leastSigBits);
  }

  /**
   * byte[] を URLセーフなBase64文字列にエンコードします（パディングなし）。
   *
   * @param bytes エンコード対象のバイナリデータ
   * @return Base64エンコード文字列
   */
  public static String toBase64(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * URLセーフな Base64 文字列をバイナリデータ（byte[]）にデコードします。
   * <p>
   * 例：UUIDのBase64文字列（パディングなし）を元のbyte[16]形式に変換します。
   * <p>
   * 不正なBase64文字列が渡された場合は、詳細なメッセージを含む IllegalArgumentException をスローします。
   *
   * @param base64 デコード対象のBase64文字列（URLセーフ、パディングなし）
   * @return デコードされたバイナリデータ（通常は UUID を表す byte[16]）
   * @throws IllegalArgumentException Base64形式が不正な場合（例："illegal base64 character" 等）
   */
  public static byte[] fromBase64(String base64) {
    try {
      return Base64.getUrlDecoder().decode(base64);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Base64の形式が不正です（" + ex.getMessage() + "）", ex);
    }
  }

  /**
   * UUIDをBase64文字列に変換します。
   *
   * @param uuid UUIDオブジェクト
   * @return Base64文字列（URLセーフ、パディングなし）
   */
  public static String toBase64(UUID uuid) {
    return toBase64(fromUUID(uuid));
  }

  /**
   * Base64文字列からUUIDを復元します。
   *
   * @param base64 Base64文字列（URLセーフ、パディングなし）
   * @return UUIDオブジェクト
   */
  public static UUID fromBase64ToUUID(String base64) {
    return toUUID(fromBase64(base64));
  }

  /**
   * 動作確認用のmainメソッド。
   * Base64文字列からUUID形式を表示します。
   */
  public static void main(String[] args) {
    String base64Id = "GdgYbbFeRU6A70yPTVUN2A"; // 例: Webで受け取ったID
    UUID uuid = fromBase64ToUUID(base64Id);
    System.out.println("Base64: " + base64Id);
    System.out.println("UUID形式: " + uuid.toString());
    System.out.println("MySQL用: UNHEX(REPLACE('" + uuid.toString() + "', '-', ''))");
  }
}
