package raisetech.student.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 受講生の基本情報を表すデータ転送オブジェクト（DTO）。
 *
 * <p>登録・更新・検索時のリクエストおよびレスポンスで使用されるクラスであり、
 * バリデーションアノテーションにより、リクエストデータの整合性を保証します。
 */
@Schema(description = "受講生の基本情報 DTO")
@Data
@NoArgsConstructor // ← 引数なしのデフォルトコンストラクタも自動生成
@AllArgsConstructor
public class StudentDto {

  /**
   * 受講生ID（UUID文字列表現）。
   *
   * <p>新規登録時は省略可能で、取得・更新時に使用されます。
   */
  @Schema(
      description = "受講生ID（UUID文字列表現、更新時などに使用）",
      format = "uuid",
      example = "123e4567-e89b-12d3-a456-426614174000")
  private String studentId;

  /**
   * 氏名（必須、最大100文字）。
   */
  @Schema(description = "氏名", example = "山田 太郎", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "名前は必須です。")
  @Size(max = 100, message = "名前は100文字以内で入力してください。")
  private String fullName;

  /**
   * ふりがな（必須）。
   */
  @Schema(description = "ふりがな", example = "やまだ たろう", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "ふりがなは必須です。")
  private String furigana;

  /**
   * ニックネーム（必須）。
   */
  @Schema(description = "ニックネーム", example = "タロウ")
  @NotBlank(message = "ニックネームは必須です。")
  private String nickname;

  /**
   * メールアドレス（必須、形式チェックあり）。
   */
  @Schema(description = "メールアドレス", example = "taro@example.com")
  @NotBlank(message = "メールアドレスを入力してください。")
  @Email(message = "メールアドレス形式が不正です。")
  private String email;

  /**
   * 居住地（任意）。
   */
  @Schema(description = "居住地", example = "Osaka,韓国")
  private String location;

  /**
   * 年齢（任意、0以上）。
   */
  @Schema(description = "年齢", example = "25")
  @Min(value = 0, message = "年齢は0以上で入力してください。")
  private Integer age;

  /**
   * 性別（必須）。
   */
  @Schema(description = "性別", example = "Male、Female,Non-binary")
  @NotBlank(message = "性別は必須です。")
  private String gender;

  /**
   * 備考（任意）。
   */
  @Schema(description = "備考", example = "特記事項なし")
  private String remarks;

  /**
   * 論理削除フラグ。trueの場合、削除された状態を表します。
   */
  @Schema(description = "論理削除フラグ。true の場合、削除された状態を示します。", example = "false")
  private Boolean deleted;
}
