package raisetech.student.management.data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 受講生の情報を保持するエンティティクラス。
 *
 * <p>このクラスはデータベースの「students」テーブルに対応し、 基本情報（氏名、ふりがな、メールアドレス等）に加えて 論理削除のためのフラグと削除日時も保持します。
 */
@Schema(description = "受講生エンティティ（データベース保存用の学生情報）")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

  /**
   * 受講生ID（UUID（DBではBINARY(16)として格納））
   */
  @Schema(
      description = "受講生ID（DBではBINARY(16)で格納）",
      format = "uuid",
      example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
  private UUID studentId;

  /**
   * 氏名
   */
  @Schema(description = "氏名", example = "山田 太郎")
  private String fullName;

  /**
   * ふりがな
   */
  @Schema(description = "ふりがな", example = "やまだ たろう")
  private String furigana;

  /**
   * ニックネーム
   */
  @Schema(description = "ニックネーム", example = "タロウ")
  private String nickname;

  /**
   * メールアドレス
   */
  @Schema(description = "メールアドレス", example = "taro.yamada@example.com")
  private String email;

  /**
   * 居住地（都道府県など）
   */
  @Schema(description = "居住地（都道府県など）", example = "Osaka,韓国")
  private String location;

  /**
   * 年齢
   */
  @Schema(description = "年齢", example = "25")
  private Integer age;

  /**
   * 性別
   */
  @Schema(description = "性別", example = "Male")
  private String gender;

  /**
   * 備考
   */
  @Schema(description = "備考・自由記述欄", example = "メモや特記事項")
  private String remarks;

  /**
   * 登録日時
   */
  @Schema(description = "登録日時", example = "2025-04-01 10:00:00")
  private LocalDateTime createdAt;

  /**
   * 論理削除された日時（削除されていない場合は null）
   */
  @Schema(description = "削除日時（論理削除時のみ値が入る）", example = "2025-06-01 12:30:00", nullable = true)
  private LocalDateTime deletedAt;

  /**
   * 削除フラグ（true：削除済み、false：有効）
   */
  @Schema(description = "論理削除フラグ。trueの場合、削除された状態を表します。", example = "false")
  private Boolean deleted;

  /**
   * この受講生を論理削除します。
   *
   * <p>削除フラグを true に設定し、削除日時を現在時刻に更新します。
   */
  public void softDelete() {
    this.deleted = true;
    this.deletedAt = LocalDateTime.now();
  }

  /**
   * この受講生を復元します。
   *
   * <p>削除フラグを false に設定し、削除日時を null にリセットします。
   */
  public void restore() {
    this.deleted = false;
    this.deletedAt = null;
  }
}
