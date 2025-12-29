package raisetech.student.management.data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 受講生コースごとの申込状況を保持するエンティティ。
 *
 * <p>DB: student_courses_application_status テーブルに対応。
 * course_id に対して 1:1 で status を管理します。
 */
@Schema(description = "受講生コース申込状況")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseApplicationStatus {

  @Schema(description = "申込状況ID", format = "uuid",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID applicationStatusId;

  @Schema(description = "コースID（DBではBINARY(16)）", format = "uuid",
      example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
  private UUID courseId;

  @Schema(description = "申込状況", example = "PROVISIONAL")
  private String status;

  @Schema(description = "作成日時")
  private LocalDateTime createdAt;

  @Schema(description = "更新日時")
  private LocalDateTime updatedAt;
}

