package raisetech.student.management.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 受講生の詳細情報（基本情報＋コース一覧）を表すデータ転送オブジェクト（DTO）。
 * <p>
 * APIのレスポンスやリクエストで、受講生の基本情報と関連するコース情報を
 * 一括でやり取りするために使用されます。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailDto {

  /**
   * 受講生の基本情報（ID、氏名、メールアドレスなど）。
   */
  private StudentDto student;

  /**
   * 受講生が登録しているコース情報のリスト。
   */
  private List<StudentCourseDto> courses;
}

