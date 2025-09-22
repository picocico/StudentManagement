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
  /** UUID -> 16バイト配列 */
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
  public static byte[] fromBase64Raw(String base64) {
    if (base64 == null || base64.isBlank()) {
      throw new IllegalArgumentException("UUIDの形式が不正です");
    }
    // 1) URL-safe デコード（パディング補正）
    try {
      String normalized = padIfNeeded(base64);
      return Base64.getUrlDecoder().decode(normalized);
    } catch (IllegalArgumentException ignore) {
      // 2) 通常Base64で再試行
      try {
        return Base64.getDecoder().decode(base64);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("UUIDの形式が不正です", e);
      }
    }
  }

  //========================
  // 「UUIDとしての」Base64 ⇄ byte[16]/UUID
  //========================

  /**
   * Base64文字列（URLセーフ/通常のどちらでも可）を
   * UUIDバイナリ（byte[16]）にデコードします。<br>
   * 16バイトでない場合は IllegalArgumentException を投げます。
   *
   * @param base64 Base64文字列（URLセーフ推奨、パディングなしでも可）
   * @return UUIDを表す byte[16]
   * @throws IllegalArgumentException 形式不正または16バイト以外
   */
  public static byte[] fromBase64(String base64) {
    try {
      // URLセーフ/標準どちらでもOKにしたい場合は前処理で '-'→'+'、'_'→'/' など補正してから decode しても良い
      byte[] bytes = Base64.getUrlDecoder().decode(base64);
      if (bytes.length != 16) {
        throw new IllegalArgumentException("UUIDの形式が不正です");
      }
      return bytes;
    } catch (IllegalArgumentException ex) {
      // Base64自体が不正な場合の文言
      String msg = ex.getMessage();
      if (msg == null || msg.isBlank()) msg = "Base64の形式が不正です";
      throw new IllegalArgumentException(msg, ex);
    }
  }

  /**
   * UUIDをBase64文字列（URLセーフ、パディングなし）に変換します。
   *
   * @param uuid UUIDオブジェクト
   * @return Base64文字列
   */
  public static String toBase64(UUID uuid) {
    return toBase64(fromUUID(uuid));
  }

  /**
   * Base64文字列（URLセーフ/通常）からUUIDを復元します。
   *
   * @param base64 Base64文字列（URLセーフ推奨、パディングなしでも可）
   * @return UUIDオブジェクト
   * @throws IllegalArgumentException 形式不正または16バイト以外
   */
  public static UUID fromBase64ToUUID(String base64) {
    return toUUID(fromBase64(base64));
  }

  //========================
  // Helpers
  //========================

  /**
   * Base64の長さを4の倍数にするために '=' を補う（URLセーフのとき用）。
   */
  private static String padIfNeeded(String s) {
    int mod = s.length() % 4;
    return mod == 0 ? s : s + "=".repeat(4 - mod);
  }

  //========================
  // 動作確認用
  //========================

  /**
   * 動作確認用のmainメソッド。
   * Base64文字列からUUID形式を表示します。
   */
  public static void main(String[] args) {
    String base64Id = "GdgYbbFeRU6A70yPTVUN2A"; // 例: Webで受け取ったID
    UUID uuid = fromBase64ToUUID(base64Id);
    System.out.println("Base64: " + base64Id);
    System.out.println("UUID:   " + uuid);
    System.out.println("Back to Base64: " + toBase64(uuid));
  }
}
