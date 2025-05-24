package raisetech.student.management.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import raisetech.student.management.data.Student;

/**
 * 受講生情報に関するデータベース操作を行うMyBatisリポジトリインターフェースです。
 * <p>
 * 受講生の登録、更新、検索、削除などを提供し、XMLマッパーファイルでSQLを制御します。
 */
@Mapper
public interface StudentRepository {

  /**
   * 動的な検索条件（ふりがな、削除状態）に基づいて受講生情報を検索します。
   *
   * @param furigana        ふりがなによる部分一致検索条件（nullまたは空文字は無視）
   * @param includeDeleted  論理削除された受講生も含めるかどうか
   * @param deletedOnly     論理削除された受講生のみ取得するかどうか
   * @return 条件に一致する受講生情報のリスト
   */
  List<Student> searchStudents(@Param("furigana") String furigana, @Param("includeDeleted") boolean includeDeleted, @Param("deletedOnly") boolean deletedOnly);

  /**
   * 受講生IDで受講生情報を取得します。
   *
   * @param studentId 受講生ID
   * @return 該当する受講生情報（存在しない場合は null）
   */
  Student findById(@Param("studentId") byte[] studentId);

  /**
   * 新しい受講生情報を登録します。
   *
   * @param student 登録する受講生情報
   */
  void insertStudent(Student student);

  /**
   * 既存の受講生情報を更新します。
   *
   * @param student 更新する受講生情報
   */
  void updateStudent(Student student);

  /**
   * 受講生IDで該当する受講生情報を物理削除します。
   *
   * @param studentId 物理削除対象の受講生ID
   */
  void forceDeleteStudent(@Param("studentId") byte[] studentId);
}




