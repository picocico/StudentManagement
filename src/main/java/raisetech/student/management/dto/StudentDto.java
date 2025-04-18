package raisetech.student.management.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor // ← 引数なしのデフォルトコンストラクタも自動生成
@AllArgsConstructor // ← 全フィールド引数のコンストラクタを自動生成
public class StudentDto {

  private String studentId;

  @NotBlank(message = "名前は必須です。")
  @Size(max = 100, message = "名前は100文字以内で入力してください。")
  private String fullName;

  @NotBlank(message = "ふりがなは必須です。")
  private String furigana;

  @NotBlank(message = "ニックネームは必須です。")
  private String nickname;

  @NotBlank(message = "メールアドレスを入力してください。")
  @Email(message = "メールアドレス形式が不正です。")
  private String email;

  private String location;

  @Min(value = 0, message = "年齢は0以上で入力してください。")
  private Integer age;

  @NotBlank(message = "性別は必須です。")
  private String gender;

  private String remarks;
  private Boolean deleted;
}


