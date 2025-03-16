package raisetech.student.management.data;

import java.time.LocalDateTime;
import java.util.UUID;
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
  private String fullName;
  private String furigana;
  private String nickname;
  private String email;
  private String location;
  private int age;
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




