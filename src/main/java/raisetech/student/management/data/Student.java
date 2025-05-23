package raisetech.student.management.data;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 受講生の情報を保持するエンティティクラス。
 * <p>
 * このクラスはデータベースの「students」テーブルに対応し、
 * 基本情報（氏名、ふりがな、メールアドレス等）に加えて
 * 論理削除のためのフラグと削除日時も保持します。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

  /** 学生ID（BINARY型、UUIDの16バイト配列） */
  private byte[] studentId;

  /** 氏名 */
  private String fullName;

  /** ふりがな */
  private String furigana;

  /** ニックネーム */
  private String nickname;

  /** メールアドレス */
  private String email;

  /** 居住地（都道府県など） */
  private String location;

  /** 年齢 */
  private Integer age;

  /** 性別 */
  private String gender;

  /** 備考 */
  private String remarks;

  /** 登録日時 */
  private LocalDateTime createdAt;

  /** 論理削除された日時（削除されていない場合は null） */
  private LocalDateTime deletedAt;

  /** 削除フラグ（true：削除済み、false：有効） */
  private Boolean deleted;

  /**
   * この受講生を論理削除します。
   * <p>
   * 削除フラグを true に設定し、削除日時を現在時刻に更新します。
   */
  public void softDelete() {
    this.deleted = true;
    this.deletedAt = LocalDateTime.now();
  }

  /**
   * この受講生を復元します。
   * <p>
   * 削除フラグを false に設定し、削除日時を null にリセットします。
   */
  public void restore() {
    this.deleted = false;
    this.deletedAt = null;
  }
}





