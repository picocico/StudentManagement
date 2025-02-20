package raisetech.student.management;

import java.util.List; // List の正しい import
import org.apache.ibatis.annotations.Delete; // 以下、MyBatis の import
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StudentRepository {

  // 全ての学生を取得する
  @Select("SELECT * FROM student")
  List<Student> findAll(); // List<Student> を返す

  // IDで検索する
  @Select("SELECT * FROM student WHERE id = #{id}")
  Student searchById(int id);

  // 名前で検索する
  @Select("SELECT * FROM student WHERE name = #{name} LIMIT 1")
  Student searchByName(String name);

  // 新しい学生を登録する
  @Insert("INSERT INTO student (name, age) VALUES (#{name}, #{age})")
  void registerStudent(@Param("name") String name, @Param("age") int age);

  // IDで更新する
  @Update("UPDATE student SET name = #{name}, age = #{age} WHERE id = #{id}")
  void updateStudent(@Param("id") int id, @Param("name") String name, @Param("age") int age);

  // 名前で削除
  @Delete("DELETE FROM student WHERE name = #{name}")
  void deleteStudentByName(String name);

  // IDで削除
  @Delete("DELETE FROM student WHERE id = #{id}")
  void deleteStudentById(int id);
}
