package raisetech.student.management.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class StudentCourseRepositoryTest {

  @Autowired StudentCourseRepository sut;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired StudentRepository studentRepository; // FK用に受講生も1人作る

  /** テスト用の受講生を1件INSERTし、そのstudent_id(byte[16])を返すヘルパー。 */
  private UUID insertTestStudentAndReturnId() {
    UUID id = UUID.randomUUID();

    Student s = new Student();
    s.setStudentId(id);
    s.setFullName("テスト 受講生");
    s.setFurigana("てすと じゅこうせい");
    s.setNickname("テスト");
    s.setEmail("course-" + System.nanoTime() + "@example.com");
    s.setLocation("Osaka");
    s.setAge(20);
    s.setGender("Male");
    s.setRemarks("コーステスト用");
    s.setDeleted(false);

    studentRepository.insertStudent(s);
    return id;
  }

  private static byte[] bytes(UUID u) {
    var bb = java.nio.ByteBuffer.allocate(16);
    bb.putLong(u.getMostSignificantBits());
    bb.putLong(u.getLeastSignificantBits());
    return bb.array();
  }

  /** テスト用の StudentCourse を1件生成する共通ヘルパー（フル版） */
  private StudentCourse newCourse(
      UUID studentId, String courseName, LocalDate start, LocalDate end) {
    StudentCourse c = new StudentCourse();
    c.setCourseId(UUID.randomUUID());
    c.setStudentId(studentId);
    c.setCourseName(courseName);
    c.setStartDate(start); // ★ここが今回のポイント
    c.setEndDate(end); // endDate は任意
    return c;
  }

  // よく使う「お任せ」版（今回のテストではこっちを使う）
  private StudentCourse newCourse(UUID studentId, String courseName) {
    return newCourse(studentId, courseName, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
  }

  @Test
  void findAllCourses_全件取得できること() {
    List<StudentCourse> actual = sut.findAllCourses();

    Integer expectedBoxed =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM student_courses", Integer.class);
    int expected = Objects.requireNonNull(expectedBoxed, "count must not be null");
    assertThat(actual).hasSize(expected);

    assertThat(actual).isNotNull().isNotEmpty();
    assertThat(actual)
        .allSatisfy(
            c -> {
              assertThat(c.getCourseId()).isNotNull();
              assertThat(c.getCourseName()).isNotBlank();
              assertThat(c.getStartDate()).isNotNull();
            });
  }

  @Test
  void findCoursesByStudentId_特定受講生のコースのみ取得できること() {
    UUID studentId1 = insertTestStudentAndReturnId();
    UUID studentId2 = insertTestStudentAndReturnId();

    // ★ コースエンティティを作成（2引数版でOK）
    StudentCourse c1 = newCourse(studentId1, "Javaコース");
    StudentCourse c2 = newCourse(studentId1, "AWSコース");
    StudentCourse c3 = newCourse(studentId2, "Pythonコース");

    // ★ 実際に DB に INSERT しないと、find しても出てこないので注意！
    sut.insertCourses(List.of(c1, c2, c3));

    List<StudentCourse> actual = sut.findCoursesByStudentId(studentId1);

    assertThat(actual)
        .hasSize(2)
        .extracting(StudentCourse::getCourseName)
        .containsExactlyInAnyOrder("Javaコース", "AWSコース");
  }

  @Test
  void insertCourses_複数コースを一括登録できること() {
    UUID studentId = insertTestStudentAndReturnId();

    StudentCourse c1 =
        newCourse(studentId, "Javaコース", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31));

    StudentCourse c2 =
        newCourse(studentId, "AWSコース", LocalDate.of(2025, 4, 1), LocalDate.of(2025, 6, 30));

    sut.insertCourses(List.of(c1, c2));

    // Assert: studentId に紐づくコースが2件増えていることなどを確認
    List<StudentCourse> actual = sut.findCoursesByStudentId(studentId);
    // 事前件数を取ってないなら「2件存在すること」でもOK
    assertThat(actual).hasSize(2);
  }

  @Test
  void insertCourses_存在しない受講生IDを指定した場合はDataIntegrityViolationException() {
    UUID nonExistingStudentId = UUID.randomUUID();

    // NOT NULL をすべて満たした上で FK だけ不正にする
    StudentCourse c =
        newCourse(
            nonExistingStudentId, "不正コース", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

    assertThatThrownBy(() -> sut.insertCourses(List.of(c)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void insertIfNotExists_同一studentId_courseNameは2回呼んでも1件のまま_DBで検証すること() {
    UUID studentId = insertTestStudentAndReturnId();
    String courseName = "Javaコース";

    StudentCourse c1 =
        newCourse(studentId, courseName, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31));
    StudentCourse c2 =
        newCourse(
            studentId, courseName, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 3, 31)); // わざと違う値

    // Act
    sut.insertIfNotExists(c1);
    sut.insertIfNotExists(c2);

    // Assert：件数は1のまま
    assertThat(countByStudentAndCourseName(studentId, courseName)).isEqualTo(1);

    // no-op なので「最初の値のまま」を確認（仕様固定に効く）
    StudentCourse saved = loadByStudentAndCourseName(studentId, courseName);
    assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2025, 1, 1));
    assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2025, 3, 31));
  }

  @Test
  void insertIfNotExists_存在しない組み合わせなら登録されること() {
    UUID studentId = insertTestStudentAndReturnId();

    StudentCourse course =
        newCourse(studentId, "Javaコース", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31));

    sut.insertIfNotExists(course);

    List<StudentCourse> actual = sut.findCoursesByStudentId(studentId);
    assertThat(actual)
        .singleElement() // 要素がちょうど1件であることも同時に検証
        .extracting(StudentCourse::getCourseName)
        .isEqualTo("Javaコース");
  }

  @Test
  void deleteCoursesByStudentId_紐づくコースが全て削除されること() {
    UUID studentId = insertTestStudentAndReturnId();

    StudentCourse c1 = newCourse(studentId, "Java基礎");
    StudentCourse c2 = newCourse(studentId, "AWS入門");

    // 実際にINSERT
    sut.insertCourses(List.of(c1, c2));

    // 削除前に2件あることを確認
    assertThat(sut.findCoursesByStudentId(studentId)).hasSize(2);

    // 削除実行
    sut.deleteCoursesByStudentId(studentId);

    // 削除後は0件になっていること
    assertThat(sut.findCoursesByStudentId(studentId)).isEmpty();
  }

  @Test
  void deleteCoursesByStudentId_該当コースが無いIDでも例外にならないこと() {
    UUID nonExistingStudentId = UUID.randomUUID();

    int beforeSize = sut.findAllCourses().size();

    // 例外が出ないことだけ確認
    sut.deleteCoursesByStudentId(nonExistingStudentId);

    // 念のためテーブル全体件数が変わっていないか確認してもOK
    int afterSize = sut.findAllCourses().size();
    assertThat(afterSize).isEqualTo(beforeSize);
  }

  // --------------------------------------------------------------
  // DB確認用ヘルパー
  // --------------------------------------------------------------
  private int countByStudentAndCourseName(UUID studentId, String courseName) {
    Integer cnt =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM student_courses WHERE student_id = ? AND course_name = ?",
            Integer.class,
            bytes(studentId), // BINARY(16)なら bytes() を使う
            courseName);
    return cnt == null ? 0 : cnt;
  }

  private StudentCourse loadByStudentAndCourseName(UUID studentId, String courseName) {
    return jdbcTemplate.queryForObject(
        """
            SELECT course_id, student_id, course_name, start_date, end_date, created_at
            FROM student_courses
            WHERE student_id = ? AND course_name = ?
            """,
        (rs, rowNum) -> {
          StudentCourse c = new StudentCourse();
          // rs.getBytes("course_id") -> UUID 変換が必要なら decode ヘルパーを用意
          c.setCourseName(rs.getString("course_name"));
          c.setStartDate(rs.getDate("start_date").toLocalDate());
          var end = rs.getDate("end_date");
          c.setEndDate(end == null ? null : end.toLocalDate());
          return c;
        },
        bytes(studentId),
        courseName);
  }
}
