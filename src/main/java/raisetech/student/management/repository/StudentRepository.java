package raisetech.student.management.repository;

import java.util.List;
import org.apache.ibatis.annotations.*;
import raisetech.student.management.data.Student;

/**
 * 受講生情報に関するデータベース操作を行うMyBatisリポジトリ。
 */
@Mapper
public interface StudentRepository {

  /**
   * 受講生IDで1件の受講生情報を取得します。
   *
   * @param studentId 検索する受講生ID
   * @return 該当する受講生（存在しない場合は null）
   */
  @Select("SELECT * FROM students WHERE student_id = #{studentId}")
  @Results({
      @Result(property = "studentId", column = "student_id"),
      @Result(property = "fullName", column = "full_name"),
      @Result(property = "furigana", column = "furigana"),
      @Result(property = "nickname", column = "nickname"),
      @Result(property = "email", column = "email"),
      @Result(property = "location", column = "location"),
      @Result(property = "age", column = "age"),
      @Result(property = "gender", column = "gender"),
      @Result(property = "remarks", column = "remarks"),
      @Result(property = "deleted", column = "is_deleted"),
      @Result(property = "deletedAt", column = "deleted_at")
  })
  Student findById(String studentId);

  /**
   * 新しい受講生情報を登録します。
   *
   * @param student 登録する受講生情報
   */
  @Insert("""
      INSERT INTO students (student_id, full_name, furigana, nickname, email, location, age, gender, remarks, created_at, is_deleted)
      VALUES (#{studentId}, #{fullName}, #{furigana}, #{nickname}, #{email}, #{location}, #{age}, #{gender}, #{remarks}, now(), #{deleted})
      """)
  void insertStudent(Student student);

  /**
   * 既存の受講生情報を更新します。
   *
   * @param student 更新する受講生情報
   */
  @Update("""
      UPDATE students SET full_name = #{fullName}, furigana = #{furigana}, nickname = #{nickname},
      email = #{email}, location = #{location}, age = #{age}, gender = #{gender}, remarks = #{remarks},
      is_deleted = #{deleted}, deleted_at = #{deletedAt} WHERE student_id = #{studentId}
      """)
  void updateStudent(Student student);

  /**
   * 削除されていない全ての受講生を取得します。
   *
   * @return 論理削除されていない受講生リスト
   */
  @Select("SELECT * FROM students WHERE is_deleted = false")
  List<Student> searchActiveStudents();

  /**
   * 論理削除フラグに関係なく、すべての受講生を取得します。
   *
   * @return 受講生エンティティのリスト
   */
  @Select("SELECT * FROM students")
  @Results(id = "studentResultMap", value = {
      @Result(property = "studentId", column = "student_id"),
      @Result(property = "fullName", column = "full_name"),
      @Result(property = "furigana", column = "furigana"),
      @Result(property = "nickname", column = "nickname"),
      @Result(property = "email", column = "email"),
      @Result(property = "location", column = "location"),
      @Result(property = "age", column = "age"),
      @Result(property = "gender", column = "gender"),
      @Result(property = "remarks", column = "remarks"),
      @Result(property = "createdAt", column = "created_at"),
      @Result(property = "deletedAt", column = "deleted_at"),
      @Result(property = "deleted", column = "is_deleted")
  })
  List<Student> findAllStudents();

  /**
   * 論理削除された受講生のみを取得します。
   *
   * @return 削除済み受講生のリスト
   */
  @Select("SELECT * FROM students WHERE is_deleted = true")
  List<Student> findDeletedStudents();

  /**
   * 指定されたふりがなに部分一致する受講生を検索します。
   *
   * @param furigana 検索対象のふりがな文字列
   * @return 一致する受講生リスト
   */
  @Select("SELECT * FROM students WHERE furigana LIKE CONCAT('%', #{furigana}, '%') AND is_deleted = false")
  List<Student> findByFurigana(String furigana);

  /**
   * ふりがなで部分一致検索（論理削除された学生も含む）。
   *
   * @param furigana 検索対象のふりがな
   * @return 一致する学生のリスト
   */
  @Select("SELECT * FROM students WHERE furigana LIKE CONCAT('%', #{furigana}, '%')")
  List<Student> findByFuriganaIncludingDeleted(@Param("furigana") String furigana);

  /**
   * 指定されたふりがなに一致する論理削除された受講生のみを検索します。
   *
   * @param furigana 検索対象のふりがな（部分一致）
   * @return 条件に一致する削除済み受講生のリスト
   */
  @Select("SELECT * FROM students WHERE furigana LIKE CONCAT('%', #{furigana}, '%') AND is_deleted = true")
  List<Student> findDeletedStudentsByFurigana(@Param("furigana") String furigana);

  /**
   * 受講生IDで該当する受講生情報を物理削除します。
   *
   * @param studentId 削除対象の受講生ID
   */
  @Delete("DELETE FROM students WHERE student_id = #{studentId}")
  void deleteById(String studentId);
}



