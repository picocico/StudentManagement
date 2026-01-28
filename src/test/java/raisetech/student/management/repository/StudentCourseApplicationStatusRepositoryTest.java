package raisetech.student.management.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Types;
import java.time.LocalDate;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class StudentCourseApplicationStatusRepositoryTest {

  @Autowired StudentCourseApplicationStatusRepository statusRepository;

  @Autowired DataSource dataSource;

  private JdbcTemplate jdbc;

  private UUID studentId;
  private UUID courseId;

  @BeforeEach
  void setUp() {
    jdbc = new JdbcTemplate(dataSource); // ←ここで必ず初期化

    studentId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    courseId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

    // students
    jdbc.update(
        """
            INSERT INTO students(
              student_id, full_name, furigana, nickname, email, location, age, gender, remarks, is_deleted
            ) VALUES (?,?,?,?,?,?,?,?,?,?)
            """,
        ps -> {
          ps.setBytes(1, bytes(studentId));
          ps.setString(2, "テスト 太郎");
          ps.setString(3, "てすと たろう");
          ps.setString(4, "たろ");
          ps.setString(5, "test@example.com");
          ps.setString(6, "大阪");
          ps.setInt(7, 30);
          ps.setString(8, "Male");
          ps.setString(9, "備考");
          ps.setBoolean(10, false);
        });

    // student_courses
    jdbc.update(
        """
            INSERT INTO student_courses(
              course_id, student_id, course_name, start_date, end_date
            ) VALUES(?,?,?,?,?)
            """,
        ps -> {
          ps.setBytes(1, bytes(courseId));
          ps.setBytes(2, bytes(studentId));
          ps.setString(3, "Javaコース");
          ps.setObject(4, LocalDate.of(2025, 1, 1));
          ps.setObject(5, null);
        });
  }

  // --------------------------------------------------------------
  // 1) insertProvisionalIfAbsent: 未登録なら PROVISIONAL を作成
  // --------------------------------------------------------------
  @Test
  void insertProvisionalIfAbsent_未登録なら1件作成されstatusがPROVISIONALになること() {
    UUID statusId = UUID.randomUUID();

    int inserted = statusRepository.insertProvisionalIfAbsent(statusId, courseId);
    assertThat(inserted).isEqualTo(1);

    String status =
        jdbc.queryForObject(
            "SELECT status FROM student_courses_application_status WHERE course_id = ?",
            new Object[] {bytes(courseId)},
            new int[] {Types.BINARY},
            String.class);

    assertThat(status).isEqualTo("PROVISIONAL");
  }

  // --------------------------------------------------------------
  // 2) insertProvisionalIfAbsent: 既存なら status が維持されること
  // --------------------------------------------------------------
  @Test
  void insertProvisionalIfAbsent_既存ならstatusがPROVISIONALのまま維持されること() {
    UUID firstId = UUID.randomUUID();
    UUID secondId = UUID.randomUUID();

    int first = statusRepository.insertProvisionalIfAbsent(firstId, courseId);
    statusRepository.insertProvisionalIfAbsent(secondId, courseId);

    assertThat(first).isEqualTo(1);

    String status =
        jdbc.queryForObject(
            "SELECT status FROM student_courses_application_status WHERE course_id = ?",
            new Object[] {bytes(courseId)},
            new int[] {Types.BINARY},
            String.class);

    assertThat(status).isEqualTo("PROVISIONAL");
  }

  // --------------------------------------------------------------
  // 3) upsertStatus: insert→update で status が更新されること
  // --------------------------------------------------------------
  @Test
  void upsertStatus_未作成なら作成され既存ならstatusが更新されること() {
    statusRepository.upsertStatus(UUID.randomUUID(), courseId, "PROVISIONAL");
    statusRepository.upsertStatus(UUID.randomUUID(), courseId, "IN_PROGRESS");

    String status =
        jdbc.queryForObject(
            "SELECT status FROM student_courses_application_status WHERE course_id = ?",
            new Object[] {bytes(courseId)},
            new int[] {Types.BINARY},
            String.class);

    assertThat(status).isEqualTo("IN_PROGRESS");
  }

  // --------------------------------------------------------------
  // 4) deleteByStudentId: 指定studentIdに紐づくstatusだけ削除されること
  // --------------------------------------------------------------
  @Test
  void deleteByStudentId_指定studentIdのコースに紐づくstatusだけ削除されること() {
    statusRepository.upsertStatus(UUID.randomUUID(), courseId, "PROVISIONAL");

    int deleted = statusRepository.deleteByStudentId(studentId);
    assertThat(deleted).isGreaterThanOrEqualTo(1);

    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM student_courses_application_status WHERE course_id = ?",
            new Object[] {bytes(courseId)},
            new int[] {Types.BINARY},
            Integer.class);

    assertThat(count).isZero();
  }

  // UUID → BINARY(16)
  private static byte[] bytes(UUID u) {
    var bb = java.nio.ByteBuffer.allocate(16);
    bb.putLong(u.getMostSignificantBits());
    bb.putLong(u.getLeastSignificantBits());
    return bb.array();
  }
}
