package raisetech.student.management.dto;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 受講生の登録・更新リクエストに使用されるデータ転送オブジェクト（DTO）。
 * <p>
 * 受講生の基本情報と受講コース情報をまとめて送信するための構造であり、
 * バリデーションアノテーションによりネストされたオブジェクトも検証されます。
 */
@Data
@NoArgsConstructor // ← 引数なしのデフォルトコンストラクタも自動生成
@AllArgsConstructor // ← 全フィールド引数のコンストラクタを自動生成
public class StudentRegistrationRequest {

  /**
   * 登録または更新対象の受講生情報。
   * <p>
   * {@code @Valid} により、内部の各フィールドにもバリデーションが適用されます。
   */
  @Valid
  private StudentDto student;

  /**
   * 登録または更新対象のコース情報一覧。
   * <p>
   * 空リストも許容されますが、各要素に対して {@code @Valid} による検証が行われます。
   */
  @Valid
  private List<StudentCourseDto> courses;

  /**
   * 登録時に削除フラグを明示的に指定する場合に使用。
   * <p>
   * 通常は {@code false}（未削除）として登録されます。
   */
  private boolean deleted;

  private boolean appendCourses; // ← trueならコースを追加、falseなら置き換え

  /*
   このメソッドは不要 （@Dataがあるため、手動Getterは不要）
   public boolean isAppendCourses() {
   return appendCourses;
   }
  */
}





