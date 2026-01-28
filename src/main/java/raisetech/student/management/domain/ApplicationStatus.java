package raisetech.student.management.domain;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;

@Getter
public enum ApplicationStatus {
  PROVISIONAL("仮申込"),
  FORMAL("本申込"),
  IN_PROGRESS("受講中"),
  COMPLETED("受講終了");

  private final String label;

  ApplicationStatus(String label) {
    this.label = label;
  }

  /** API/DBで扱うステータスコード（例: "IN_PROGRESS"）。 */
  public String getCode() {
    return name();
  }

  /** コード文字列から ApplicationStatus を取得します（不正/空なら empty）。 */
  public static Optional<ApplicationStatus> fromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(values()).filter(v -> v.name().equals(code)).findFirst();
  }
}
