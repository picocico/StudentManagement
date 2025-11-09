package raisetech.student.management.util;

import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component("idCodec") // 既存のBean名を維持したい場合は名前を固定。不要なら外してOK
public class DefaultIdCodec implements IdCodec {

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
    return UUIDUtil.fromUUID(decodeUuidOrThrow(base64));
  }
}
