package raisetech.student.management.data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
   * コースID（UUID を BINARY(16) として保持する 16 バイト配列）。
   *
   * <p>データベースでは BINARY(16) で格納され、API 層では UUID 文字列表現に変換して扱われます。
   */
  @Schema(
      description = "コースID（UUIDをBINARY形式で格納した16バイト配列）",
      format = "byte")
  private byte[] courseId;

  /**
   * 受講生ID（UUID を BINARY(16) として保持する 16 バイト配列）。
   *
   * <p>このコースに紐づく受講生のID。
   */
  @Schema(
      description = "受講生ID（UUIDをBINARY形式で格納した16バイト配列）",
      format = "byte")
  private byte[] studentId;

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
