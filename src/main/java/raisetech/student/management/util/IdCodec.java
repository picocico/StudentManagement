package raisetech.student.management.util;

import java.util.UUID;

public interface IdCodec {

  /**
   * Base64文字列をUUIDに変換する（不正な場合は例外を投げる）
   */
  UUID decodeUuidOrThrow(String base64);

  /**
   * Base64文字列をUUIDのバイト配列（16バイト）に変換する（不正な場合は例外を投げる）
   */
  byte[] decodeUuidBytesOrThrow(String base64);

  /**
   * バイト配列をBase64文字列に変換する
   */
  String encodeId(byte[] id);

  /**
   * Base64文字列をバイト配列に変換する
   */
  byte[] decode(String id);

  /**
   * 新規ID（UUID由来の16バイト配列）を生成する
   */
  byte[] generateNewIdBytes();
}
