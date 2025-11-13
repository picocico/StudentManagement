package raisetech.student.management.util;

import java.util.UUID;

public interface IdCodec {

  /**
   * URL-safe Base64 文字列から UUID を復元します。
   *
   * <p>Base64として不正、または UUID 16バイトに復元できない場合は
   * {@link IllegalArgumentException} をスローします。
   */
  UUID decodeUuidOrThrow(String base64);

  /**
   * URL-safe Base64 文字列から UUID 由来の16バイト配列を復元します。
   *
   * <p>Base64として不正、または 16バイト以外の長さになった場合は
   * {@link IllegalArgumentException} をスローします。
   *
   * <p>DB の PK（学生ID・コースIDなど UUID/BINARY(16) 前提のID）向けです。
   */
  byte[] decodeUuidBytesOrThrow(String base64);

  /**
   * UUID 由来の16バイト配列を URL-safe Base64 文字列にエンコードします。
   *
   * <p>16バイト以外が渡された場合は {@link IllegalArgumentException} をスローします。
   */
  String encodeId(byte[] id);

  /**
   * URL-safe Base64 文字列を単純にバイト配列へデコードします。
   *
   * <p>長さチェックは行いません。Base64として不正な場合のみ
   * {@link IllegalArgumentException} をスローします。
   *
   * <p>UUID 固定長を要求しない汎用的なID（文字列ID など）の復号に利用します。
   */
  byte[] decode(String id);

  /**
   * 新規ID（UUID由来の16バイト配列）を生成します。
   */
  byte[] generateNewIdBytes();
}
