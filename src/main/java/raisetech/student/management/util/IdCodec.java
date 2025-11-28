package raisetech.student.management.util;

import java.util.UUID;

/**
 * UUID/BINARY(16) ベースの ID を UUID 文字列表現と相互変換するためのユーティリティ契約インターフェース。
 *
 * <p>契約:
 * <ul>
 *   <li>{@code decodeUuidOrThrow} / {@code decodeUuidBytesOrThrow}:
 *     <ul>
 *       <li>入力は標準的な UUID 文字列表現（例: {@code 123e4567-e89b-12d3-a456-426614174000}）を前提とする</li>
 *       <li>UUID 文字列として不正な場合は {@link IllegalArgumentException} をスローする</li>
 *     </ul>
 *   </li>
 *   <li>{@code encodeId}:
 *     <ul>
 *       <li>引数は UUID/BINARY(16) の 16 バイト配列を前提とする</li>
 *       <li>16 バイト以外が渡された場合は {@link IllegalArgumentException} をスローする</li>
 *     </ul>
 *   </li>
 *   <li>{@code generateNewIdBytes}:
 *     <ul>
 *       <li>{@link UUID#randomUUID()} 相当のランダム UUID を元にした 16 バイト配列を返す</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>ドメイン層では、必要に応じてこれらの例外を {@code InvalidIdFormatException}
 * などにラップして利用します。
 */
public interface IdCodec {

  /**
   * UUID 文字列表現から {@link UUID} を復元します。
   *
   * <p>UUID 文字列として不正な場合は {@link IllegalArgumentException} をスローします。
   *
   * @param uuidString UUID の文字列表現
   * @return 復元された UUID
   * @throws IllegalArgumentException 文字列が UUID として不正な場合
   */
  UUID decodeUuidOrThrow(String uuidString);

  /**
   * UUID 文字列表現から UUID 由来の 16 バイト配列を復元します。
   *
   * <p>UUID 文字列として不正な場合は {@link IllegalArgumentException} をスローします。
   *
   * @param uuidString UUID の文字列表現
   * @return 復元された 16 バイト配列
   * @throws IllegalArgumentException 文字列が UUID として不正な場合
   */
  byte[] decodeUuidBytesOrThrow(String uuidString);

  /**
   * UUID 由来の 16 バイト配列を UUID 文字列表現にエンコードします。
   *
   * <p>引数が {@code null} の場合は {@code null} を返します。
   * それ以外で 16 バイト以外の長さの配列が渡された場合は {@link IllegalArgumentException} をスローします。
   *
   * @param id UUID を表す 16 バイト配列、null や 16 バイト以外は許可しない。
   * @return UUID 文字列表現
   * @throws IllegalArgumentException 引数が null または 16 バイト以外の場合
   */
  String encodeId(byte[] id);

  /**
   * 新規 ID（UUID 由来の 16 バイト配列）を生成します。
   *
   * <p>戻り値の配列は必ず長さ 16 であり、{@link UUID#randomUUID()} 相当のランダム値に基づくことを想定します。
   *
   * @return 新規に生成された 16 バイトの UUID バイト配列
   */
  byte[] generateNewIdBytes();
}
