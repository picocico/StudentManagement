package raisetech.student.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 受講生が登録しているコース情報を表すデータ転送オブジェクト（DTO）。
 *
 * <p>クライアントとのリクエストおよびレスポンス時に使用されるクラスで、 {@code StudentCourse} エンティティの必要な情報のみを提供します。
 */
@Schema(description = "受講生コース情報 DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseDto {

  /**
   * コースID（UUID 文字列表現）。
   *
   * <p>フロントエンドと通信する際に、BINARY型のUUIDをURLセーフな文字列として扱うために使用されます。
   */
  @Schema(
      description = "コースID（UUID 文字列表現、更新や既存識別用）",
      example = "123e4567-e89b-12d3-a456-426614174000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String courseId;

  /**
   * コース名。
   */
  @Schema(description = "コース名", example = "Java基礎", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "コース名は必須です")
  private String courseName;

  /**
   * コースの開始日。
   */
  @Schema(description = "コースの開始日（形式：yyyy-MM-dd）", example = "2025-04-01")
  private LocalDate startDate;

  /**
   * コースの終了日。
   */
  @Schema(description = "コースの終了日（形式：yyyy-MM-dd）", example = "2025-06-30")
  private LocalDate endDate;
}
