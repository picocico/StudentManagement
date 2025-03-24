package raisetech.student.management.data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student {

  private String studentId;

  @NotBlank(message = "名前は必須です。")
  @Size(max = 100, message = "名前は50文字以内で入力してください。")
  private String fullName;

  @NotBlank(message = "ふりがなは必須です。")
  private String furigana;

  @NotBlank(message = "ニックネームは必須です。")
  private String nickname;

  @NotNull(message = "メールアドレスは必須です。")
  @NotBlank(message = "メールアドレスを入力してください。")
  @Email(message = "メールアドレス形式で入力してください。")
  @Pattern(
      regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
      message = "メールアドレス形式が不正です。"
  )
  private String email;
  private String location;
  private int age;

  @NotBlank(message = "性別は必須です。")
  private String gender;
  private String remarks = "";
  private LocalDateTime createdAt;
  private LocalDateTime deletedAt;
  private boolean isDeleted; // 明示的な論理削除フラグを追加

  /**
   * 学生が論理削除されているかどうかを判定
   *
   * @return 削除されている場合は true, そうでなければ false
   */
  public boolean isDeleted() {
    return isDeleted;
  }

  /**
   * 学生を論理削除するメソッド
   */
  public void softDelete() {
    this.isDeleted = true;
    this.deletedAt = LocalDateTime.now();
  }

  /**
   * 学生を復元するメソッド
   */
  public void restore() {
    this.isDeleted = false;
    this.deletedAt = null;
  }
}




