package raisetech.student.management.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

/**
 * 受講生が登録しているコース情報を表すデータ転送オブジェクト（DTO）。
 * <p>
 * クライアントとのリクエストおよびレスポンス時に使用されるクラスで、
 * {@code StudentCourse} エンティティの必要な情報のみを提供します。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseDto {

  /**
   * コースID（Base64エンコードされたUUID文字列）。
   * <p>
   * フロントエンドと通信する際に、BINARY型のUUIDをURLセーフな文字列として扱うために使用されます。
   */
  private String courseId;

  /**
   * コース名。
   */
  private String courseName;

  /**
   * コースの開始日。
   */
  private LocalDate startDate;

  /**
   * コースの終了日。
   */
  private LocalDate endDate;
}

