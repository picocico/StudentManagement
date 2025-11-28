package raisetech.student.management.util;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.springframework.stereotype.Component;
import raisetech.student.management.exception.InvalidIdFormatException;

/**
 * UUID/BINARY(16) と UUID 文字列表現の相互変換コンポーネント。 デフォルトの {@link IdCodec} 実装。
 *
 * <p>Bean 名を {@code "idCodec"} に固定することで、
 * 既存コードの {@code @Qualifier("idCodec")} との互換性を維持します。
 */
@Component("idCodec")
public class StudentIdCodec implements IdCodec {

  /**
   * UUID バイト列（16バイト）を UUID 文字列表現に変換する。
   *
   * @param id UUID の生 16 バイト。null や 16 バイト以外は許可しない。
   * @return UUID 文字列表現
   * @throws IllegalArgumentException id が null または 16 バイト以外の場合
   */
  @Override
  public String encodeId(byte[] id) {
    // 元々Converterにあった IDエンコードロジックをここに集約
    if (id == null) {
      throw new IllegalArgumentException("IDはnullにできません（UUID）");
    }
    if (id.length != 16) {
      throw new IllegalArgumentException("UUIDの形式が不正です");
    }
    UUID uuid = UUIDUtil.fromBytes(id);
    return uuid.toString(); // 標準的な UUID 文字列表現
  }

  /**
   * UUID 文字列表現 を {@link UUID} オブジェクトに変換します。
   *
   * @param uuidString UUID 文字列表現。（例: {@code 123e4567-e89b-12d3-a456-426614174000}）。 {@code null}
   *                   は許可されません。
   * @return 変換された {@link UUID} オブジェクト
   * @throws InvalidIdFormatException {@code uuidString} が {@code null} または 　　　　　　　　　　　　　　　　　　　　UUID
   *                                  として解釈できない場合
   */
  @Override
  public UUID decodeUuidOrThrow(String uuidString) {
    if (uuidString == null) {
      throw new InvalidIdFormatException("IDはnullにできません（UUID）");
    }
    try {
      return UUID.fromString(uuidString);
    } catch (IllegalArgumentException e) {
      // ここでメッセージを UUID 用に変更
      throw new InvalidIdFormatException("IDの形式が不正です（UUID）", e);
    }
  }

  /**
   * UUID 文字列表現を UUID 由来の 16 バイト配列に変換します。
   *
   * <p>内部的には {@link #decodeUuidOrThrow(String)} で UUID にデコードしたうえで、
   * その最上位ビット／下位ビットを 16 バイト配列として詰め直します。
   *
   * @param uuidString UUID 文字列表現（例: {@code 123e4567-e89b-12d3-a456-426614174000}）。 {@code null}
   *                   は許可されません。
   * @return UUID を表す 16 バイト配列
   * @throws InvalidIdFormatException {@code uuidString} が {@code null} または UUID として解釈できない場合
   */
  @Override
  public byte[] decodeUuidBytesOrThrow(String uuidString) {
    UUID uuid = decodeUuidOrThrow(uuidString);

    ByteBuffer bb = ByteBuffer.allocate(16);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  /**
   * 新規の UUID/BINARY(16) ID を生成します。
   *
   * <p>{@link UUID#randomUUID()} でランダムな UUID を採番し、
   * {@link UUIDUtil#toBytes(UUID)} を使って 16 バイト配列に変換して返します。
   *
   * <p>主キーの新規採番など、「まだ ID を持たないエンティティ」に対して
   * 利用することを想定しています。常に長さ 16 の配列を返し、 {@code null} を返すことはありません。
   *
   * @return 新規に採番された UUID を表す 16 バイト配列
   */
  @Override
  public byte[] generateNewIdBytes() {
    UUID uuid = UUID.randomUUID();
    return UUIDUtil.toBytes(uuid);
  }
}
