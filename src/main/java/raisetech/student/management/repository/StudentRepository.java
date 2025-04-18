package raisetech.student.management.repository;

import java.util.List;
import org.apache.ibatis.annotations.*;
import raisetech.student.management.data.Student;

/**
 * 学生情報に関するデータベース操作を行うMyBatisリポジトリ。
 */
@Mapper
public interface StudentRepository {

  /**
   * 学生IDで1件の学生情報を取得します。
   *
   * @param studentId 検索する学生ID
   * @return 該当する学生（存在しない場合は null）
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
   * 新しい学生情報を登録します。
   *
   * @param student 登録する学生情報
   */
  @Insert("""
      INSERT INTO students (student_id, full_name, furigana, nickname, email, location, age, gender, remarks, created_at, is_deleted)
      VALUES (#{studentId}, #{fullName}, #{furigana}, #{nickname}, #{email}, #{location}, #{age}, #{gender}, #{remarks}, now(), #{deleted})
      """)
  void insertStudent(Student student);

  /**
   * 既存の学生情報を更新します。
   *
   * @param student 更新する学生情報
   */
  @Update("""
      UPDATE students SET full_name = #{fullName}, furigana = #{furigana}, nickname = #{nickname},
      email = #{email}, location = #{location}, age = #{age}, gender = #{gender}, remarks = #{remarks},
      is_deleted = #{deleted}, deleted_at = #{deletedAt} WHERE student_id = #{studentId}
      """)
  void updateStudent(Student student);

  /**
   * 削除されていない全ての学生を取得します。
   *
   * @return 論理削除されていない学生リスト
   */
  @Select("SELECT * FROM students WHERE is_deleted = false")
  List<Student> searchActiveStudents();

  /**
   * 論理削除フラグに関係なく、すべての学生を取得します。
   *
   * @return 学生エンティティのリスト
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
   * 指定されたふりがなに部分一致する学生を検索します。
   *
   * @param furigana 検索対象のふりがな文字列
   * @return 一致する学生リスト
   */
  @Select("SELECT * FROM students WHERE furigana LIKE CONCAT('%', #{furigana}, '%') AND is_deleted = false")
  List<Student> findByFurigana(String furigana);

  /**
   * 学生IDで該当する学生情報を物理削除します。
   *
   * @param studentId 削除対象の学生ID
   */
  @Delete("DELETE FROM students WHERE student_id = #{studentId}")
  void deleteById(String studentId);
}



