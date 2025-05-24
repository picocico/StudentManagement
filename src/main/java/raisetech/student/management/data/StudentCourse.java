package raisetech.student.management.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 受講生とコースの関連情報を保持するエンティティクラス。
 * <p>
 * 受講生がどのコースに所属しているか、受講期間などを管理します。
 * データベースの student_courses テーブルに対応します。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourse {

  /**
   * コースID（BINARY型、Base64エンコード前の16バイト配列）。
   * <p>
   * UUIDをBINARY(16)で格納した形式。
   */
  private byte[] courseId;

  /**
   * 受講生ID（BINARY型、Base64エンコード前の16バイト配列）。
   * <p>
   * このコースに紐づく受講生のID。
   */
  private byte[] studentId;

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

  /**
   * このエントリが作成された日時。
   */
  private LocalDateTime createdAt;
}


