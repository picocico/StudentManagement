package raisetech.student.management.domain;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;

/**
 * 受講生の詳細情報（基本情報＋受講コース一覧）を表すドメインモデル。
 *
 * <p>クライアントやサービス層において、受講生と関連コースを一括で扱う際に使用します。
 */
@Getter
@Setter
public class StudentDetail {

  /** 受講生の基本情報。 */
  private Student student;

  /** 受講生に紐づくコース情報のリスト。 */
  private List<StudentCourse> studentCourse;

  /**
   * 受講生の詳細情報を生成するコンストラクタ。
   *
   * @param student 受講生の基本情報
   * @param studentCourse 受講生が登録しているコース情報のリスト
   */
  public StudentDetail(Student student, List<StudentCourse> studentCourse) {
    this.student = student;
    this.studentCourse = studentCourse;
  }
}
