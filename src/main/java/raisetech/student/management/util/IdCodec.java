package raisetech.student.management.util;

import java.util.UUID;

public interface IdCodec {

  UUID decodeUuidOrThrow(String base64);

  byte[] decodeUuidBytesOrThrow(String base64);
}
