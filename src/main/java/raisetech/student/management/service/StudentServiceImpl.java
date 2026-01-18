package raisetech.student.management.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.domain.ApplicationStatus;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.exception.ResourceNotFoundException;
import raisetech.student.management.repository.StudentCourseApplicationStatusRepository;
import raisetech.student.management.repository.StudentCourseRepository;
import raisetech.student.management.repository.StudentRepository;

/**
 * {@link StudentService} の実装クラス
 *
 * <p>受講生およびコース情報の登録・更新・削除・検索といったビジネスロジックを提供します。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

  private final StudentRepository studentRepository;
  private final StudentCourseRepository courseRepository;
  private final StudentCourseApplicationStatusRepository statusRepository;
  private final StudentConverter converter;

  /**
   * 受講生を登録します。
   *
   * @param student 登録する受講生エンティティ
   * @param courses 受講生に紐づくコースリスト（複数可）
   */
  @Override
  @Transactional
  public void registerStudent(Student student, List<StudentCourse> courses) {
    studentRepository.insertStudent(student);

    if (courses != null && !courses.isEmpty()) {
      UUID studentId = student.getStudentId();
      for (StudentCourse c : courses) {
        c.setStudentId(studentId); // 念のため上書き
      }
      courseRepository.insertCourses(courses);

      for (StudentCourse c : courses) {
        statusRepository.insertProvisionalIfAbsent(UUID.randomUUID(), c.getCourseId());
      }
    }
  }

  /**
   * 受講生情報と受講コース情報を「全体更新（全置換）」します。
   *
   * <p>本メソッドは、指定された受講生IDのレコードに対して以下を同一トランザクションで実行します。
   *
   * <ol>
   *   <li><b>受講生（student）</b>：{@code studentRepository.updateStudent(student)} により更新します。
   *   <li><b>受講コース（courses）</b>：いったん当該受講生に紐づくコースを全削除し、 引数 {@code courses} の内容で再登録（置換）します。
   * </ol>
   *
   * <h3>コース置換のルール</h3>
   *
   * <ul>
   *   <li>{@code courses == null} または {@code courses.isEmpty()} の場合：コースは全削除され、0件になります。
   *   <li>コースが存在する場合：各 {@link StudentCourse} に {@code studentId} を再セットしてから一括登録します。
   * </ul>
   *
   * <h3>申込状況（application status）の登録</h3>
   *
   * <p>コース登録後、各コースについて「仮申込（PROVISIONAL）」を未登録の場合のみ登録します （既に存在する場合は何もしません）。
   *
   * <p><b>存在しない受講生ID</b>の場合、更新件数が0となるため {@link ResourceNotFoundException} を送出します。
   *
   * <p><b>トランザクション</b>：本処理は {@link Transactional} により同一トランザクションで実行されます。
   * 途中で例外が発生した場合、学生更新・コース削除/挿入・申込状況登録はロールバックされます。
   *
   * @param student 更新対象の受講生エンティティ（{@code studentId} 必須）
   * @param courses 更新後に紐づける受講コースの一覧（{@code null} / 空の場合は「全削除のみ」）
   * @return 更新後の受講生エンティティ（DBから再取得した最新状態）
   * @throws NullPointerException {@code student} が {@code null} の場合
   * @throws IllegalArgumentException {@code student.getStudentId()} が {@code null} の場合
   * @throws ResourceNotFoundException 指定IDの受講生が存在しない場合、または整合性確保のため再取得に失敗した場合
   */
  @Override
  @Transactional
  public Student updateStudentWithCourses(Student student, List<StudentCourse> courses) {
    Objects.requireNonNull(student, "student must not be null");
    UUID studentId = student.getStudentId();
    if (studentId == null) {
      throw new IllegalArgumentException("studentId must not be null");
    }

    // 1) いきなり UPDATE して件数を見る
    int updatedCount = studentRepository.updateStudent(student);
    if (updatedCount == 0) {
      String idForLog = studentId.toString();
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }

    // 2) コース全削除 → 一括Insert（メソッド名：deleteCoursesByStudentId / insertCourses）
    courseRepository.deleteCoursesByStudentId(studentId);
    if (courses != null && !courses.isEmpty()) {
      for (StudentCourse sc : courses) {
        sc.setStudentId(studentId); // 念のため上書き
      }
      courseRepository.insertCourses(courses);
      for (StudentCourse c : courses) {
        statusRepository.insertProvisionalIfAbsent(UUID.randomUUID(), c.getCourseId());
      }
    }

    // 3) 最新の学生を再取得（null返し仕様に合わせる）
    Student updated = studentRepository.findById(studentId);
    if (updated == null) {
      // 直前で更新しているので通常起きないが、整合性確保のため
      throw new ResourceNotFoundException("student", "studentId");
    }
    return updated;
  }

  /**
   * 受講生情報を部分更新（PATCH）します。
   *
   * <p>本メソッドは、リクエスト内容に応じて「受講生の基本情報」と「受講コース情報」を部分的に更新し、 最終状態（学生＋コース）を再取得して返却します。
   *
   * <h3>処理概要</h3>
   *
   * <ol>
   *   <li><b>存在確認</b>：指定された {@code studentId} の受講生が存在することを確認します（存在しない場合は404）。
   *   <li><b>学生本体の更新</b>：{@code req.getStudent() != null} の場合のみ、既存情報にマージして {@code
   *       updateStudentSelective} により部分更新します（null値による上書きを防止）。
   *   <li><b>コースの更新</b>：{@code req.getCourses() == null} の場合はコースを一切変更しません。 {@code req.getCourses()
   *       != null} の場合のみ、{@code appendCourses} の値により動作を切り替えます。
   *   <li><b>再取得</b>：更新後にDBから最新の学生・コース情報を再取得し、DTOに変換して返します。
   * </ol>
   *
   * <h3>appendCourses の意味</h3>
   *
   * <ul>
   *   <li>{@code true}（または未指定＝デフォルト）：<b>追加・更新</b>のみを行い、リクエストに含まれない既存コースは保持します。
   *   <li>{@code false}：<b>差し替え</b>（既存 - リクエスト を削除）を行い、リクエスト内容に合わせて追加・更新します。
   * </ul>
   *
   * <h3>courses の null / 空配列の扱い</h3>
   *
   * <ul>
   *   <li>{@code courses == null}：フィールド未指定扱いとして<b>コースは触りません</b>。
   *   <li>{@code courses.isEmpty()}：
   *       <ul>
   *         <li>{@code appendCourses == false} の場合：<b>全削除</b>（0件に置換）。
   *         <li>{@code appendCourses == true} の場合：<b>no-op</b>（変更なし）。
   *       </ul>
   * </ul>
   *
   * <h3>コース更新の詳細</h3>
   *
   * <ul>
   *   <li>{@code courseId == null}：新規追加として本メソッド側で {@link UUID#randomUUID()} を採番し、コースを登録します。
   *   <li>{@code courseId != null}：既存コース更新として、指定の {@code courseId} が当該受講生に紐づくことを確認したうえで更新します。
   *   <li>申込状況（application status）は、指定がある場合に更新します。新規追加時に未指定の場合は {@code "PROVISIONAL"}
   *       をデフォルトとして登録します。
   * </ul>
   *
   * <p><b>トランザクション</b>：本処理は {@link Transactional} により同一トランザクションで実行されます。
   *
   * @param studentId 更新対象の受講生ID（UUID）
   * @param req 部分更新リクエスト（student / courses / appendCourses を含む）
   * @param studentIdString レスポンスDTOに設定する受講生ID文字列（パスで受け取ったUUID文字列表現など）
   * @return 更新後の受講生詳細DTO（学生＋コース）
   * @throws IllegalArgumentException {@code studentId} が {@code null} の場合（{@code findStudentById}
   *     実装に依存）
   * @throws ResourceNotFoundException 受講生が存在しない場合、または指定した {@code courseId} が当該受講生に紐づかない場合
   */
  @Override
  @Transactional
  public StudentDetailDto patchStudent(
      UUID studentId, StudentRegistrationRequest req, String studentIdString) {

    // 1) 存在確認（404）
    findStudentById(studentId);

    // 2) student が来ていれば更新
    patchStudentEntityIfPresent(studentId, req);

    // 3) courses が来ていれば更新（nullなら触らない）
    patchCoursesIfPresent(studentId, req);

    // 4) 最新を返す
    return loadLatestDetail(studentId, studentIdString);
  }

  // --------------------
  // private helpers
  // --------------------

  private void patchStudentEntityIfPresent(UUID studentId, StudentRegistrationRequest req) {
    if (req.getStudent() == null) {
      return;
    }

    Student existing = findStudentById(studentId);
    Student update = converter.toEntity(req.getStudent());

    converter.mergeStudent(existing, update);
    studentRepository.updateStudentSelective(existing);
  }

  private void patchCoursesIfPresent(UUID studentId, StudentRegistrationRequest req) {
    if (req.getCourses() == null) {
      return; // フィールド未指定 → 触らない
    }

    final boolean append = req.isAppendCourses(); // null -> true ルール
    final List<?> courses = req.getCourses();

    // ★空配列の扱いを明示
    if (courses.isEmpty()) {
      if (!append) {
        // 差し替えモードで空配列 → 全削除
        courseRepository.deleteCoursesByStudentId(studentId);
      }
      // append=true で空配列 → no-op
      return;
    }

    // 既存コース（所有チェック＆差し替え削除に使う）
    List<StudentCourse> existingCourses = courseRepository.findCoursesByStudentId(studentId);
    List<UUID> existingIds = existingCourses.stream().map(StudentCourse::getCourseId).toList();

    // リクエストを entity 化（studentId強制セット / courseId nullはnullのまま）
    List<StudentCourse> reqCourses = converter.toCourseEntities(studentId, req.getCourses());

    List<StudentCourse> toInsert =
        reqCourses.stream().filter(c -> c.getCourseId() == null).toList();
    List<StudentCourse> toUpdate =
        reqCourses.stream().filter(c -> c.getCourseId() != null).toList();

    // append=false なら差し替え削除（existing - request）
    if (!append) {
      List<UUID> requestIds = toUpdate.stream().map(StudentCourse::getCourseId).toList();

      List<UUID> deleteIds = existingIds.stream().filter(id -> !requestIds.contains(id)).toList();

      if (!deleteIds.isEmpty()) {
        courseRepository.deleteCoursesByCourseIds(studentId, deleteIds);
      }
    }

    // 追加（courseId==null）
    for (StudentCourse c : toInsert) {
      insertCourseWithStatus(studentId, c);
    }

  /**
   * 受講生の基本情報のみを更新します。
   *
   * <p>このメソッドでは、氏名、メールアドレス、年齢などの基本属性のみが更新対象となり、 コース情報（student_coursesテーブル）は一切変更されません。
   *
   * <p>PATCHリクエストで「コースの追加」のみを行う場合に併用され、 既存のコース情報を保持したまま、受講生の属性情報だけを変更したいケースで使用します。
   *
   * @param student 更新対象の受講生エンティティ（student_idを含む必要があります）
   */
  @Override
  @Transactional
  public void updateStudentInfoOnly(Student student) {
    Objects.requireNonNull(student, "student must not be null");

  private void insertCourseWithStatus(UUID studentId, StudentCourse c) {
    c.setStudentId(studentId);
    c.setCourseId(UUID.randomUUID());

    courseRepository.insertCourses(List.of(c));

    String status = (c.getApplicationStatus() != null) ? c.getApplicationStatus() : "PROVISIONAL";
    statusRepository.upsertStatus(UUID.randomUUID(), c.getCourseId(), status);
  }

  private void updateCourseWithOptionalStatus(
      UUID studentId, List<UUID> existingIds, StudentCourse c) {
    c.setStudentId(studentId);

    // 所有チェック（他人courseId更新の事故防止）
    if (!existingIds.contains(c.getCourseId())) {
      throw new ResourceNotFoundException("courseId " + c.getCourseId() + " はこの受講生に紐づきません");
    }

    boolean hasCourseFields =
        c.getCourseName() != null || c.getStartDate() != null || c.getEndDate() != null;
    boolean hasStatus = c.getApplicationStatus() != null;

    // A) ステータスだけ更新（コース情報は触らない）
    if (!hasCourseFields) {
      if (hasStatus) {
        statusRepository.upsertStatus(UUID.randomUUID(), c.getCourseId(), c.getApplicationStatus());
      }
      return;
    }

    // B) コース情報も更新（Selective）
    int updated = courseRepository.updateCourseSelective(c);
    if (updated == 0) {
      throw new ResourceNotFoundException("courseId " + c.getCourseId() + " が見つかりません");
    }

    // status もあれば更新
    if (hasStatus) {
      statusRepository.upsertStatus(UUID.randomUUID(), c.getCourseId(), c.getApplicationStatus());
    }
  }

  private StudentDetailDto loadLatestDetail(UUID studentId, String studentIdString) {
    Student latest = findStudentById(studentId);
    List<StudentCourse> latestCourses = searchCoursesByStudentId(studentId);
    return converter.toDetailDto(latest, latestCourses, studentIdString);
  }

  /**
   * 検索条件に基づいて受講生詳細情報リストを取得します。
   *
   * @param furigana ふりがな検索（省略可能）
   * @param includeDeleted 論理削除済みも含めるか
   * @param deletedOnly 論理削除済みのみ取得するか
   * @return 受講生詳細DTOリスト
   */
  @Override
  public List<StudentDetailDto> getStudentList(
      String furigana,
      boolean includeDeleted,
      boolean deletedOnly,
      ApplicationStatus applicationStatus) {

    if (includeDeleted && deletedOnly) {
      throw new IllegalArgumentException("includeDeletedとdeletedOnlyの両方をtrueにすることはできません");
    }

    String statusCode = (applicationStatus == null) ? null : applicationStatus.name();

    // 動的SQLにより1本化されたリポジトリメソッドを呼び出し
    List<Student> students =
        studentRepository.searchStudents(furigana, includeDeleted, deletedOnly, statusCode); // 1本化！

    List<UUID> studentIds =
        students.stream().map(Student::getStudentId).filter(Objects::nonNull).toList();

    List<StudentCourse> courses =
        studentIds.isEmpty()
            ? List.of()
            : courseRepository.findCoursesByStudentIds(studentIds, statusCode);
    return converter.toDetailDtoList(students, courses);
  }

  /**
   * 受講生IDで受講生情報を取得します。
   *
   * @param studentId 受講生ID（UUID）
   * @return 該当する受講生
   * @throws ResourceNotFoundException 該当する受講生が存在しない場合
   */
  @Override
  public Student findStudentById(UUID studentId) {
    if (studentId == null) {
      throw new IllegalArgumentException("UUIDの形式が不正です");
    }

    Student student = studentRepository.findById(studentId);
    if (student == null) {
      // ログ用に UUID 文字列を生成
      String idForLog = studentId.toString();
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }
    return student;
  }

  /**
   * 受講生IDに紐づくコース情報を取得します。
   *
   * @param studentId 受講生ID（UUID）
   * @return コースリスト
   */
  @Override
  public List<StudentCourse> searchCoursesByStudentId(UUID studentId) {
    return courseRepository.findCoursesByStudentId(studentId);
  }

  /**
   * 全コース情報を取得します。
   *
   * @return コースリスト
   */
  @Override
  public List<StudentCourse> searchAllCourses() {
    return courseRepository.findAllCourses();
  }

  /**
   * 受講生を論理削除します。
   *
   * @param studentId 受講生ID（UUID）
   */
  @Override
  @Transactional
  public void softDeleteStudent(UUID studentId) {
    Student student = studentRepository.findById(studentId);

    // 対象の受講生が存在しない場合は例外をスロー
    if (student == null) {
      throw new ResourceNotFoundException(
          "Student not found for ID: " + studentId);
    }

    // すでに論理削除済みでなければ、削除処理を行う
    if (!Boolean.TRUE.equals(student.getDeleted())) {
      student.softDelete();
      int updated = studentRepository.updateStudent(student);
      if (updated == 0) {
        // ここは通常起こりにくいが、整合性の保険として
        throw new IllegalStateException("論理削除に失敗しました: " +
            student.getStudentId());
      }
      log.info("論理削除完了 - studentId: {}", student.getStudentId());
    }
  }

  /**
   * 論理削除された受講生を復元します。
   *
   * @param studentId 受講生ID（UUID）
   * @throws ResourceNotFoundException 受講生が存在しない場合
   */
  @Override
  @Transactional
  public void restoreStudent(UUID studentId) {
    Student student = studentRepository.findById(studentId);
    String idForLog = (studentId != null) ? studentId.toString() : "null";
    if (student == null) {
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }

    log.debug(
        "Before restore: studentId = {}, deleted = {}, deletedAt = {}",
        idForLog,
        student.getDeleted(),
        student.getDeletedAt());

    if (Boolean.TRUE.equals(student.getDeleted())) {
      student.restore();
      int updated = studentRepository.updateStudent(student);
      log.debug(
          "After restore: studentId = {}, deleted = {}, deletedAt = {}",
          idForLog,
          student.getDeleted(),
          student.getDeletedAt());
      if (updated == 0) {
        throw new IllegalStateException("復元に失敗しました: " + idForLog);
      }
    }
  }

  /**
   * 指定された受講生IDに該当する受講生情報および関連するコース情報を物理削除します。
   *
   * <p>この操作はデータベースから完全に削除され、復元はできません。 主に管理者向けの操作として利用されます。
   *
   * @param studentId 物理削除対象の受講生ID（UUID）
   * @throws ResourceNotFoundException 該当する受講生が存在しない場合にスローされます
   */
  @Override
  @Transactional
  public void forceDeleteStudent(UUID studentId) {
    String idForLog = (studentId != null) ? studentId.toString() : "null";
    // 1. 先にコースを削除（存在しないIDなら 0件削除で終わるだけ）
    courseRepository.deleteCoursesByStudentId(studentId);

    // 2. 物理削除して、削除件数を受け取る
    int deleted = studentRepository.forceDeleteStudent(studentId);

    // 3. 1件も削除されなければ「存在しないID」と判断
    if (deleted == 0) {
      // Transactional によりコース削除もロールバックされるので、副作用は残らない
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }

    // 4. 正常に1件削除された場合はログを出して終了
    log.info("物理削除完了 - studentId: {}", idForLog);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public Student updateStudentWithCourses(Student student, List<StudentCourse> courses) {
    Objects.requireNonNull(student, "student must not be null");
    UUID studentId = student.getStudentId();
    if (studentId == null) {
      throw new IllegalArgumentException("studentId must not be null");
    }

    // 1) いきなり UPDATE して件数を見る
    int updatedCount = studentRepository.updateStudent(student);
    if (updatedCount == 0) {
      String idForLog = studentId.toString();
      throw new ResourceNotFoundException("受講生ID " + idForLog + " が見つかりません。");
    }

    // 2) コース全削除 → 一括Insert（メソッド名：deleteCoursesByStudentId / insertCourses）
    courseRepository.deleteCoursesByStudentId(studentId);
    if (courses != null && !courses.isEmpty()) {
      for (StudentCourse sc : courses) {
        sc.setStudentId(studentId); // 念のため上書き
      }
      courseRepository.insertCourses(courses);
      for (StudentCourse c : courses) {
        statusRepository.insertProvisionalIfAbsent(UUID.randomUUID(), c.getCourseId());
      }
    }

    // 3) 最新の学生を再取得（null返し仕様に合わせる）
    Student updated = studentRepository.findById(studentId);
    if (updated == null) {
      // 直前で更新しているので通常起きないが、整合性確保のため
      throw new ResourceNotFoundException("student", "studentId");
    }
    return updated;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<StudentCourse> getCoursesByStudentId(UUID studentId) {
    if (studentId == null) {
      throw new IllegalArgumentException("studentId must not be null");
    }
    // メソッド名：findCoursesByStudentId
    return courseRepository.findCoursesByStudentId(studentId);
  }

  @Override
  @Transactional
  public void replaceCourses(UUID studentId, List<StudentCourse> newCourses) {
    // 受講生の存在チェック（必要なら既存メソッド呼び出し）
    findStudentById(studentId);

    // 既存コースを全削除
    courseRepository.deleteCoursesByStudentId(studentId);

    // 新規があれば挿入
    if (newCourses != null && !newCourses.isEmpty()) {
      for (StudentCourse c : newCourses) {
        c.setStudentId(studentId); // 念のためセット
      }
      courseRepository.insertCourses(newCourses);
      for (StudentCourse c : newCourses) {
        statusRepository.insertProvisionalIfAbsent(UUID.randomUUID(), c.getCourseId());
      }
    }
  }
}
