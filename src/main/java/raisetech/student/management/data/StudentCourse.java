package raisetech.student.management.data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 受講生とコースの関連情報を保持するエンティティクラス。
 *
 * <p>受講生がどのコースに所属しているか、受講期間などを管理します。 データベースの student_courses テーブルに対応します。
 */
@Schema(description = "受講生コース情報")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourse {

  /**
   * コースID（UUID（DBではBINARY(16)として格納））。
   *
   * <p>データベースでは BINARY(16) で格納され、アプリケーション内部では UUID型 として扱います。
   * API（JSON）では UUIDは文字列形式 で送受信されます。
   */
  @Schema(
      description = "コースID（DBではBINARY(16)で格納）",
      format = "uuid",
      example = "550e8400-e29b-41d4-a716-446655440000"
  )
  private UUID courseId;

  /**
   * 受講生ID（UUID（DBではBINARY(16)として格納））。
   *
   * <p>このコースに紐づく受講生のID。
   */
  @Schema(
      description = "受講生ID（DBではBINARY(16)で格納）",
      format = "uuid",
      example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
  )
  private UUID studentId;

  /**
   * コース名。
   */
  @Schema(description = "コース名", example = "Spring Boot入門")
  private String courseName;

  /**
   * コースの開始日。
   */
  @Schema(description = "コースの開始日", example = "2025-04-01")
  private LocalDate startDate;

  /**
   * コースの終了日。
   */
  @Schema(description = "コースの終了日", example = "2025-06-30")
  private LocalDate endDate;

  /**
   * このエントリが作成された日時。
   */
  @Schema(description = "データ作成日時", example = "2025-04-01 10:15:30")
  private LocalDateTime createdAt;
}
