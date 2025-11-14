package raisetech.student.management.util;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * UUID/BINARY(16) と URL-safe Base64 の相互変換コンポーネント。
 *
 * <p>{@code @Component("idCodec")} として Bean 名を固定しており、
 * 既存の {@code @Qualifier("idCodec")} や XML 設定との互換性を保つために名前を明示しています。
 */
@Component("idCodec")
public class StudentIdCodec implements IdCodec {

  @Override
  public String encodeId(byte[] id) {
    // 元々Converterにあった encodeBase64 のロジックをここに移植
    if (id == null) {
      return null;
    }
    if (id.length != 16) {
      throw new IllegalArgumentException("UUIDの形式が不正です");
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(id);
  }

  @Override
  public byte[] decode(String base64) {
    // Base64 → byte[] の単純なデコード（長さチェックはここではしない想定）
    if (base64 == null) {
      return null;
    }
    try {
      return Base64.getUrlDecoder().decode(base64);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("studentId(Base64)が不正です", e);
    }
  }

  @Override
  public byte[] generateNewIdBytes() {
    // 元々Converterにあった generateRandomBytes のロジックをここに移植
    UUID uuid = UUID.randomUUID();
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  @Override
  public UUID decodeUuidOrThrow(String base64) {
    try {
      byte[] bytes = Base64.getUrlDecoder().decode(base64);
      if (bytes.length != 16) {
        throw new IllegalArgumentException("Base64デコード結果の長さが16バイトではありません");
      }
      return UUIDUtil.toUUID(bytes);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("studentId(Base64)が不正です", e);
    }
  }

  @Override
  public byte[] decodeUuidBytesOrThrow(String base64) {
    // 元々Converterにあった decodeBase64ToBytes や decodeIdOrThrow のロジックをここに移植
    // UUID形式チェックや例外スローもここで行う
    return UUIDUtil.fromUUID(decodeUuidOrThrow(base64));
  }
}
