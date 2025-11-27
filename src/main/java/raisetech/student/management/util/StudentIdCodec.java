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

  @Override
  public String encodeId(byte[] id) {
    // 元々Converterにあった IDエンコードロジックをここに集約
    if (id == null) {
      return null;
    }
    if (id.length != 16) {
      throw new IllegalArgumentException("UUIDの形式が不正です");
    }
    UUID uuid = UUIDUtil.fromBytes(id);
    return uuid.toString(); // 標準的な UUID 文字列表現
  }

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

  @Override
  public byte[] decodeUuidBytesOrThrow(String uuidString) {
    if (uuidString == null) {
      throw new InvalidIdFormatException("IDはnullにできません（UUID）");
    }
    try {
      UUID uuid = decodeUuidOrThrow(uuidString);
      ByteBuffer bb = ByteBuffer.allocate(16);
      bb.putLong(uuid.getMostSignificantBits());
      bb.putLong(uuid.getLeastSignificantBits());
      return bb.array();
    } catch (IllegalArgumentException ex) {
      throw new InvalidIdFormatException("IDの形式が不正です（UUID）", ex);
    }
  }

  @Override
  public byte[] generateNewIdBytes() {
    UUID uuid = UUID.randomUUID();
    return UUIDUtil.toBytes(uuid);
  }
}
