package raisetech.student.management.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;
import lombok.Setter;

/**
 * 受講生の登録・更新リクエストに使用されるデータ転送オブジェクト（DTO）。
 * <p>
 * 受講生の基本情報と受講コース情報をまとめて送信するための構造であり、
 * バリデーションアノテーションによりネストされたオブジェクトも検証されます。
 */
@Schema(description = "受講生登録リクエスト（基本情報＋コース情報）")
@Getter
@Setter
@Data
@NoArgsConstructor // ← 引数なしのデフォルトコンストラクタも自動生成
public class StudentRegistrationRequest {
  /**
   * 登録または更新対象の受講生情報。
   * <p>
   * {@code @Valid} により、内部の各フィールドにもバリデーションが適用されます。
   */
  @Schema(description = "受講生の基本情報", requiredMode = Schema.RequiredMode.REQUIRED)
  @Valid
  @NotNull(message = "student は必須です")
  @JsonProperty("student")
  private StudentDto student;

  /**
   * 登録または更新対象のコース情報一覧。
   * <p>
   * 空リストも許容されますが、各要素に対して {@code @Valid} による検証が行われます。
   */
  @ArraySchema(
      schema = @Schema(implementation = StudentCourseDto.class,
      description = "受講生の受講コース一覧")
  )
  @Valid
  @NotNull(message = "courses は必須です")
  @JsonProperty("courses")
  private List<StudentCourseDto> courses;

  /**
   * 登録時に削除フラグを明示的に指定する場合に使用。
   * <p>
   * 通常は {@code false}（未削除）として登録されます。
   */
  @Schema(description = "true にすると論理削除状態として登録・更新される（通常は使用しない）",
      example = "false")
  @JsonProperty("deleted")
  private boolean deleted;

  /**
   * 既存の受講コースに追加登録するかどうかを示すフラグ。
   * <p>true の場合、新しいコースを既存のコースに追加する。
   * false の場合、既存のコースを上書きして新しいコースに置き換える。</p>
   */
  @Schema(description = "true にすると既存のコースを保持し、courses に指定されたコースを追加する",
      example = "false")
  @JsonProperty("appendCourses")
  // ★ Boolean にして @Getter/@Setter を付与
  private Boolean appendCourses;// ← trueならコースを追加、falseなら置き換え

  /**
   * 部分更新リクエストが空かどうか判定
   * （student, courses, appendCourses すべて未指定）
   */
  public boolean isPatchEmpty() {
    boolean noStudent  = (student == null);
    boolean noCourses  = (courses == null || courses.isEmpty());
    boolean noAppend   = (appendCourses == null || !appendCourses);
    // 何も意味のある変更がない → 空更新
    return noStudent && noCourses && noAppend;
  }

  /**
   * Jackson が必ずこのコンストラクタで JSON → オブジェクトを作るように明示。
   */
  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public StudentRegistrationRequest(
      @JsonProperty("student") StudentDto student,
      @JsonProperty("courses") List<StudentCourseDto> courses,
      @JsonProperty("deleted") boolean deleted,
      @JsonProperty("appendCourses") boolean appendCourses
  ) {
    this.student = student;
    this.courses = courses;
    this.deleted = deleted;
    this.appendCourses = appendCourses;
  }

  /*
   このメソッドは不要 （@Dataがあるため、手動Getterは不要）
   public boolean isAppendCourses() {
   return appendCourses;
   }
  */

}





