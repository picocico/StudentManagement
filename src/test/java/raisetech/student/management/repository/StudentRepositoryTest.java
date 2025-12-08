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
import org.springframework.dao.DataIntegrityViolationException;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;

@MybatisTest
public class StudentRepositoryTest {

  @Autowired
  private StudentRepository sut;

  @Autowired
  private StudentCourseRepository courseRepository;

  // コア：すべてのフィールドを引数で受け取る（private）
  private UUID insertStudent(
      String fullName,
      String furigana,
      String nickname,
      String email,
      String location,
      int age,
      String gender,
      String remarks
  ) {
    UUID id = UUID.randomUUID();

    Student s = new Student();
    s.setStudentId(id);
    s.setFullName(fullName);
    s.setFurigana(furigana);
    s.setNickname(nickname);
    s.setEmail(email);
    s.setLocation(location);
    s.setAge(age);
    s.setGender(gender);
    s.setRemarks(remarks);
    s.setDeleted(false);

    sut.insertStudent(s);
    return id;
  }

  // 一般用：何も考えずに「まともな1件」が欲しいとき
  private UUID insertTestStudentAndReturnId() {
    String email = "test-" + System.nanoTime() + "@example.com";
    return insertStudent(
        "テスト 太郎",
        "てすと たろう",
        "テスト",
        email,
        "東京",
        30,
        "male",
        "テスト用"
    );
  }

  // email を指定したいときだけこれを使う
  private UUID insertTestStudentAndReturnId(String email) {
    return insertStudent(
        "テスト 一郎",
        "てすと いちろう",
        "いっくん",
        email,
        "Nagoya",
        20,
        "Male",
        "テスト用レコード"
    );
  }

  @Test
  void searchStudents_受講生の全件検索が行えること() {
    List<Student> actual = sut.searchStudents(
        null,   // furigana 検索条件なし
        true,   // includeDeleted: 論理削除済みも含める
        false   // deletedOnly: 削除済みのみではない
    );
    // 検証
    assertThat(actual).hasSize(16);
  }

  @Test
  void searchStudents_論理削除を除いた受講生一覧が取得できること() {
    List<Student> actual = sut.searchStudents(
        null,
        false, // includeDeleted: 削除済みは含めない
        false  // deletedOnly: 削除済みのみではない
    );

    assertThat(actual).hasSize(14); // 16件中、2件が is_deleted = 1 の想定
  }

  @Test
  void searchStudents_論理削除された受講生のみ取得できること() {
    List<Student> actual = sut.searchStudents(
        null,
        true,  // ※ 全件＋下の deletedOnly 条件で削除のみになるはず
        true   // deletedOnly: 削除済みのみ
    );

    assertThat(actual).hasSize(2);
  }

  @Test
  void searchStudents_ふりがなで部分一致検索が行えること() {
    List<Student> actual = sut.searchStudents(
        "やまだ", // data.sql 上のふりがなに合わせて
        true,     // 削除も含める
        false
    );

    assertThat(actual)
        .singleElement()
        .extracting(Student::getFullName)
        .isEqualTo("Yamada Taro");
  }

  @Test
  void insertStudent_受講生の登録が行えること() {
    // arrange: INSERT前の件数を取得
    List<Student> before = sut.searchStudents(null, true, false);
    int beforeSize = before.size();

    // act: 1件INSERT
    UUID id = insertTestStudentAndReturnId();

    // assert: 件数が +1 されていること
    List<Student> after = sut.searchStudents(null, true, false);
    assertThat(after.size()).isEqualTo(beforeSize + 1);

    // さきほどのIDを持つレコードが存在すること
    Student saved = after.stream()
        .filter(s -> Objects.equals(s.getStudentId(), id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("登録した受講生が見つかりません"));

    // ついでに中身も確認しておく
    assertThat(saved.getDeleted()).isFalse();
  }


  @Test
  void findById_既存IDで取得できること() {
    UUID id = insertTestStudentAndReturnId(); // ← 引数なし版でOK

    Student found = sut.findById(id);

    assertThat(found).isNotNull();
    assertThat(found.getStudentId()).isEqualTo(id);
  }


  @Test
  void findById_存在しないIDの場合はnullが返ること() {
    UUID unusedId = UUID.randomUUID();

    Student actual = sut.findById(unusedId);

    assertThat(actual).isNull();
  }

  @Test
  void updateStudent_既存受講生の情報を更新できること() {
    // まずテスト用レコードをINSERTして、そのIDをもらう
    UUID id = insertTestStudentAndReturnId();

    Student student = sut.findById(id);
    assertThat(student).isNotNull();

    student.setStudentId(id);
    student.setFullName("更新 一郎");
    student.setFurigana("こうしん いちろう");
    student.setRemarks("更新済みレコード");

    int updated = sut.updateStudent(student);
    assertThat(updated).isEqualTo(1);

    // Assert
    Student reloaded = sut.findById(id);
    assertThat(reloaded.getFullName()).isEqualTo("更新 一郎");
    assertThat(reloaded.getFurigana()).isEqualTo("こうしん いちろう");
    assertThat(reloaded.getRemarks()).isEqualTo("更新済みレコード");
  }

  @Test
  void updateStudent_必須項目がnullの場合はDataIntegrityViolationExceptionが送出されること() {
    UUID id = insertTestStudentAndReturnId();

    // そのレコードを取得
    Student toUpdate = sut.findById(id);
    assertThat(toUpdate).isNotNull();

    // full_name を null にして NOT NULL 制約違反を起こさせる
    toUpdate.setFullName(null);

    // update 時に DataIntegrityViolationException が投げられること
    assertThatThrownBy(() -> sut.updateStudent(toUpdate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void updateStudent_存在しないIDの場合は0件更新となること() {
    // 未使用っぽい ID を生成
    UUID unusedId = UUID.randomUUID();

    Student student = new Student();
    student.setStudentId(unusedId);
    student.setFullName("ダミー");
    student.setFurigana("だみー");
    student.setEmail("dummy@example.com");
    student.setGender("Male");
    student.setLocation("どこか");
    student.setAge(30);

    int updated = sut.updateStudent(student);

    assertThat(updated).isEqualTo(0);
  }

  @Test
  void updateStudent_メールアドレスが重複するとDataIntegrityViolationExceptionになること() {
    // 重複させたいメールアドレス
    String duplicatedEmail = "dup-update@example.com";

    // 1人目 INSERT（data.sql の既存レコードでもOK）
    UUID id1 = insertTestStudentAndReturnId(duplicatedEmail);

    Student saved = sut.findById(id1);
    assertThat(saved).isNotNull();
    assertThat(saved.getEmail()).isEqualTo(duplicatedEmail);

    // 2人目 INSERT
    UUID id2 = insertTestStudentAndReturnId("second-update@example.com"); // メールは別のものにしておく
    Student student2 = sut.findById(id2);
    assertThat(student2).isNotNull();

    // 2人目の email を 1人目と同じにしてユニーク制約違反を起こさせる
    student2.setEmail(duplicatedEmail); // id1 と同じにするイメージ

    assertThatThrownBy(() -> sut.updateStudent(student2))
        .isInstanceOf(DataIntegrityViolationException.class); // or DuplicateKeyException
  }

  @Test
  void updateStudent_年齢が負の値の場合はDataIntegrityViolationExceptionになること() {
    UUID id = insertTestStudentAndReturnId();
    Student student = sut.findById(id);
    assertThat(student).isNotNull();

    student.setAge(-1);

    assertThatThrownBy(() -> sut.updateStudent(student))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void forceDeleteStudent_受講生が存在する場合は削除されること() {
    UUID id = insertTestStudentAndReturnId();

    int deleted = sut.forceDeleteStudent(id);

    assertThat(deleted).isEqualTo(1);
    assertThat(sut.findById(id)).isNull();
  }

  @Test
  void forceDeleteStudent_存在しないIDの場合は0件削除となること() {
    UUID unusedId = UUID.randomUUID();

    int deleted = sut.forceDeleteStudent(unusedId);

    assertThat(deleted).isEqualTo(0);
  }

  @Test
  void forceDeleteStudent_受講コースが残っている場合は外部キー制約違反で例外になること() {
    UUID id = insertTestStudentAndReturnId();

    StudentCourse course = new StudentCourse();
    course.setStudentId(id);
    course.setCourseId(UUID.randomUUID());
    course.setCourseName("Javaコース");
    course.setStartDate(LocalDate.of(2025, 1, 1));
    course.setEndDate(LocalDate.of(2025, 12, 31));
    // 他の必須項目があればセット

    courseRepository.insertCourses(List.of(course));

    // Act & Assert: 子が残った状態で親だけ消そうとすると FK エラー
    assertThatThrownBy(() -> sut.forceDeleteStudent(id))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
