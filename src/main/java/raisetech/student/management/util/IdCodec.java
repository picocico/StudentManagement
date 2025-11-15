package raisetech.student.management.util;

import java.util.UUID;

/**
 * UUID/BINARY(16) ベースのIDを Base64 文字列と相互変換するためのユーティリティ契約インターフェース。
 *
 * <p>契約:
 * <ul>
 *   <li>decodeUuidOrThrow / decodeUuidBytesOrThrow:
 *       <ul>
 *         <li>入力は URL-safe Base64 を前提とする</li>
 *         <li>Base64 として不正、または 16 バイト UUID に復元できない場合は
 *             {@link IllegalArgumentException} をスローする</li>
 *       </ul>
 *   </li>
 *   <li>encodeId:
 *       <ul>
 *         <li>引数は UUID/BINARY(16) の 16 バイト配列を前提とする</li>
 *         <li>16 バイト以外が渡された場合は {@link IllegalArgumentException} をスローする</li>
 *       </ul>
 *   </li>
 *   <li>decode:
 *       <ul>
 *         <li>長さチェックは行わず、Base64 として不正な場合にのみ {@link IllegalArgumentException} をスローする</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p>ドメイン層では、必要に応じてこれらの例外を {@code InvalidIdFormatException} などにラップして利用します。
 */
public interface IdCodec {

  /**
   * URL-safe Base64 文字列から UUID を復元します。
   *
   * <p>Base64 として不正、または UUID の 16 バイト表現に復元できない場合は
   * {@link IllegalArgumentException} をスローします。
   *
   * @param base64 URL-safe Base64 形式の UUID 文字列
   * @return 復元された UUID
   * @throws IllegalArgumentException 文字列が Base64 として不正、または UUID の 16 バイト表現にならない場合
   */
  UUID decodeUuidOrThrow(String base64);

  /**
   * URL-safe Base64 文字列から UUID 由来の 16 バイト配列を復元します。
   *
   * <p>Base64 として不正、または 16 バイト以外の長さになった場合は
   * {@link IllegalArgumentException} をスローします。
   *
   * <p>DB の主キー（学生 ID・コース ID など UUID/BINARY(16) 前提の ID）向けです。
   *
   * @param base64 URL-safe Base64 形式の ID 文字列
   * @return 復元された 16 バイトの配列
   * @throws IllegalArgumentException Base64 として不正、または 16 バイト長にならない場合
   */
  byte[] decodeUuidBytesOrThrow(String base64);

  /**
   * UUID 由来の 16 バイト配列を URL-safe Base64 文字列にエンコードします。
   *
   * <p>引数が {@code null} の場合は {@code null} を返します。
   * それ以外で 16 バイト以外の長さの配列が渡された場合は {@link IllegalArgumentException} をスローします。
   *
   * @param id UUID を表す 16 バイト配列（または {@code null}）
   * @return URL-safe Base64 文字列、または入力が {@code null} の場合は {@code null}
   * @throws IllegalArgumentException 引数が 16 バイト以外の長さの配列である場合
   */
  String encodeId(byte[] id);

  /**
   * URL-safe Base64 文字列を単純にバイト配列へデコードします。
   *
   * <p>引数が {@code null} の場合は {@code null} を返します。
   * 長さチェックは行いません。Base64 として不正な場合のみ {@link IllegalArgumentException} をスローします。
   *
   * <p>UUID 固定長を要求しない汎用的な ID（任意長の文字列 ID など）の復号に利用します。
   *
   * @param id URL-safe Base64 文字列（または {@code null}）
   * @return デコードされたバイト配列、または入力が {@code null} の場合は {@code null}
   * @throws IllegalArgumentException 文字列が Base64 として不正な場合
   */
  byte[] decode(String id);

  /**
   * 新規 ID（UUID 由来の 16 バイト配列）を生成します。
   *
   * <p>戻り値の配列は必ず長さ 16 であり、{@link UUID#randomUUID()} 相当のランダム値に基づくことを想定します。
   *
   * @return 新規に生成された 16 バイトの UUID バイト配列
   */
  byte[] generateNewIdBytes();
}
