package raisetech.student.management.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 受講生の詳細情報（基本情報＋コース一覧）を表すデータ転送オブジェクト（DTO）。
 *
 * <p>APIのレスポンスやリクエストで、受講生の基本情報と関連するコース情報を 一括でやり取りするために使用されます。
 */
@Schema(description = "受講生の詳細情報（基本情報＋コース一覧）")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailDto {

  /**
   * 受講生の基本情報（ID、氏名、メールアドレスなど）。
   */
  @Schema(description = "受講生の基本情報")
  private StudentDto student;

  /**
   * 受講生が登録しているコース情報のリスト。
   */
  @ArraySchema(
      schema = @Schema(implementation = StudentCourseDto.class,
          description = "受講生が登録しているコース情報の一覧"))
  private List<StudentCourseDto> courses;
}
