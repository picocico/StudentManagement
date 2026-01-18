package raisetech.student.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.domain.ApplicationStatus;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.exception.ResourceNotFoundException;
import raisetech.student.management.repository.StudentCourseApplicationStatusRepository;
import raisetech.student.management.repository.StudentCourseRepository;
import raisetech.student.management.repository.StudentRepository;

/**
 * {@link StudentServiceImpl} の単体テストクラス。
 *
 * <p>DB では UUID を BINARY(16) で保持しつつ、 サービス層では UUID 型として受講生情報およびコース情報を扱う前提で、 その振る舞いを検証します。
 *
 * <ul>
 *   <li>リポジトリ層への委譲が正しく行われているか
 *   <li>コースの更新・追加ロジックが意図通りに呼び出されるか
 *   <li>論理削除・復元・物理削除における例外処理/メッセージが期待通りか
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

  /**
   * 受講生リポジトリのモック。
   *
   * <p>受講生情報の永続化や検索の振る舞いをスタブ/検証するために利用します。
   */
  @Mock private StudentRepository studentRepository;

  /**
   * 受講生コースリポジトリのモック。
   *
   * <p>コース情報の取得・登録・削除などをスタブ/検証するために利用します。
   */
  @Mock private StudentCourseRepository courseRepository;

  /**
   * 受講生コースの申請状況のリポジトリのモック。
   *
   * <p>コースの申請状況の取得・登録・削除などをスタブ/検証するために利用します。
   */
  @Mock private StudentCourseApplicationStatusRepository statusRepository;

  /**
   * エンティティとDTO間の変換を行うコンバーターのモック。
   *
   * <p>サービス層からの呼び出しを検証しつつ、DTOリスト生成などをスタブします。
   */
  @Mock private StudentConverter converter;

  /**
   * テスト対象のサービス実装。
   *
   * <p>{@link InjectMocks} により、上記モックがインジェクションされた状態の {@link StudentServiceImpl} が生成されます。
   */
  @InjectMocks private StudentServiceImpl service;

  /** テストで利用する固定 UUID。 */
  private static final String UUID_STRING = "123e4567-e89b-12d3-a456-426614174000";

  /** テスト共通で使用する受講生 ID（UUID）。 */
  private UUID studentId;

  /** テスト共通で使用する受講生エンティティ。 */
  private Student student;

  @Captor ArgumentCaptor<List<UUID>> studentIdsCaptor;

  /**
   * 各テスト実行前に共通の準備を行います。
   *
   * <ul>
   *   <li>{@link #UUID_STRING} から {@link #studentId} を生成
   *   <li>{@link #student} の基本情報を初期化
   *   <li>必要に応じたダミーの {@link StudentCourse} を生成
   * </ul>
   */
  @BeforeEach
  void setUp() {
    studentId = UUID.fromString(UUID_STRING);

    student = new Student();
    student.setStudentId(studentId);
    student.setFullName("テスト　花子");
    student.setEmail("test@example.com");
    student.setAge(30);
  }

  /**
   * 受講生登録時に、コースリストが空であればコース登録が行われないことを検証します。
   *
   * <p>期待する挙動:
   *
   * <ul>
   *   <li>{@code insertStudent(student)} は 1 回呼ばれる
   *   <li>{@code insertCourses(...)} は 1 度も呼ばれない
   * </ul>
   */
  @Test
  void registerStudent_受講生登録時_コースが空ならコースは登録されないこと() {
    // コースが空のケースにする
    List<StudentCourse> emptyCourses = List.of();
    // 実行
    service.registerStudent(student, emptyCourses);

    // 検証
    // insertStudent() が1回呼ばれているかどうか？
    verify(studentRepository, times(1)).insertStudent(student);
    // insertCourses() は一度も呼ばれなかったかどうか？
    verify(courseRepository, never()).insertCourses(anyList());
    verifyNoInteractions(statusRepository);
  }

  @Test
  void updateStudentWithCourses_studentがnullならNullPointerExceptionとなること() {
    assertThatThrownBy(() -> service.updateStudentWithCourses(null, List.of()))
        .isInstanceOf(NullPointerException.class);
    verifyNoInteractions(studentRepository, courseRepository, statusRepository, converter);
  }

  @Test
  void updateStudentWithCourses_studentIdがnullならIllegalArgumentExceptionとなること() {
    Student s = new Student();
    assertThatThrownBy(() -> service.updateStudentWithCourses(s, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("studentId must not be null");
  }

  @Test
  void updateStudentWithCourses_update0件なら404の例外エラーとなること() {
    when(studentRepository.updateStudent(any(Student.class))).thenReturn(0);

    assertThatThrownBy(() -> service.updateStudentWithCourses(student, List.of()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("受講生ID " + studentId + " が見つかりません。");

    verify(studentRepository).updateStudent(student);
    verifyNoInteractions(courseRepository, statusRepository);
  }

  @Test
  void updateStudentWithCourses_coursesがnullなら全削除してinsertしないこと() {
    when(studentRepository.updateStudent(student)).thenReturn(1);
    when(studentRepository.findById(studentId)).thenReturn(student);

    service.updateStudentWithCourses(student, null);

    InOrder inOrder = inOrder(studentRepository, courseRepository);
    inOrder.verify(studentRepository).updateStudent(student);
    inOrder.verify(courseRepository).deleteCoursesByStudentId(studentId);
    verify(courseRepository, never()).insertCourses(anyList());
    verifyNoInteractions(statusRepository);
  }

  @Test
  void updateStudentWithCourses_coursesありならstudentIdを再セットしてinsertしstatusも作ること() {
    when(studentRepository.updateStudent(student)).thenReturn(1);
    when(studentRepository.findById(studentId)).thenReturn(student);

    StudentCourse c1 = new StudentCourse();
    c1.setCourseId(UUID.randomUUID());
    StudentCourse c2 = new StudentCourse();
    c2.setCourseId(UUID.randomUUID());
    List<StudentCourse> courses = List.of(c1, c2);

    service.updateStudentWithCourses(student, courses);

    verify(courseRepository).deleteCoursesByStudentId(studentId);

    // insert引数の中身を検証
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<StudentCourse>> captor = ArgumentCaptor.forClass(List.class);

    verify(courseRepository).insertCourses(captor.capture());
    List<StudentCourse> inserted = captor.getValue();
    assertThat(inserted).hasSize(2);
    assertThat(inserted).allSatisfy(sc -> assertThat(sc.getStudentId()).isEqualTo(studentId));

    verify(statusRepository, times(2)).insertProvisionalIfAbsent(any(UUID.class), any(UUID.class));
  }

  @Test
  void updateStudentWithCourses_再取得がnullならResourceNotFoundExceptionとなること() {
    when(studentRepository.updateStudent(student)).thenReturn(1);
    when(studentRepository.findById(studentId)).thenReturn(null);

    assertThatThrownBy(() -> service.updateStudentWithCourses(student, List.of()))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(studentRepository).updateStudent(student);
    verify(courseRepository).deleteCoursesByStudentId(studentId);
  }

  /**
   * getStudentList において、通常のふりがな検索時に関連メソッドが順序通り呼ばれ、 期待結果が返却されることを検証します。
   *
   * <p>条件: includeDeleted=false, deletedOnly=false
   */
  @Test
  void getStudentList_ふりがな検索_で関連メソッドが順に呼ばれ結果が返ること() {

    // 準備
    String furigana = "やまだ　たかし";
    boolean includeDeleted = false;
    boolean deletedOnly = false;
    ApplicationStatus applicationStatus = ApplicationStatus.IN_PROGRESS;

    UUID studentId = UUID.randomUUID();
    Student s = new Student();
    s.setStudentId(studentId);
    List<Student> mockStudents = List.of(s);

    List<StudentCourse> mockCourses = List.of(new StudentCourse());
    List<StudentDetailDto> expectedDtoList = List.of(new StudentDetailDto());

    when(studentRepository.searchStudents(
            furigana, includeDeleted, deletedOnly, applicationStatus.name()))
        .thenReturn(mockStudents);

    // ★ searchAllCourses() ではなく、studentIds でまとめてコース取得する想定
    when(courseRepository.findCoursesByStudentIds(anyList(), eq(applicationStatus.name())))
        .thenReturn(mockCourses);

    when(converter.toDetailDtoList(mockStudents, mockCourses)).thenReturn(expectedDtoList);

    // 実行
    List<StudentDetailDto> result =
        service.getStudentList(furigana, includeDeleted, deletedOnly, applicationStatus);

    // 主張
    assertThat(result).isEqualTo(expectedDtoList);

    // 呼び出し検証（順序付き）
    InOrder inOrder = inOrder(studentRepository, courseRepository, converter);
    inOrder
        .verify(studentRepository)
        .searchStudents(furigana, includeDeleted, deletedOnly, applicationStatus.name());

    inOrder
        .verify(courseRepository)
        .findCoursesByStudentIds(studentIdsCaptor.capture(), eq(applicationStatus.name()));
    // ★ studentIds がちゃんと作られているか検証
    assertThat(studentIdsCaptor.getValue()).containsExactly(studentId);

    inOrder.verify(converter).toDetailDtoList(eq(mockStudents), eq(mockCourses));
  }

  /** getStudentList において、論理削除を含めて検索する場合 （includeDeleted=true, deletedOnly=false）の振る舞いを検証します。 */
  @Test
  void getStudentList_論理削除含めた検索_で関連メソッドが順に呼ばれ結果が返ること() {

    // 準備
    String furigana = "さとう　じろう";
    boolean includeDeleted = true;
    boolean deletedOnly = false;
    ApplicationStatus applicationStatus = ApplicationStatus.IN_PROGRESS;

    UUID studentId = UUID.randomUUID();
    Student s = new Student();
    s.setStudentId(studentId);
    List<Student> mockStudents = List.of(s);

    List<StudentCourse> mockCourses = List.of(new StudentCourse());
    List<StudentDetailDto> expectedDtoList = List.of(new StudentDetailDto());

    when(studentRepository.searchStudents(
            furigana, includeDeleted, deletedOnly, applicationStatus.name()))
        .thenReturn(mockStudents);

    when(courseRepository.findCoursesByStudentIds(anyList(), eq(applicationStatus.name())))
        .thenReturn(mockCourses);

    when(converter.toDetailDtoList(mockStudents, mockCourses)).thenReturn(expectedDtoList);

    // 実行
    List<StudentDetailDto> result =
        service.getStudentList(furigana, includeDeleted, deletedOnly, applicationStatus);

    // 主張
    assertThat(result).isEqualTo(expectedDtoList);

    // 呼び出し検証（順序付き）
    InOrder inOrder = inOrder(studentRepository, courseRepository, converter);
    inOrder
        .verify(studentRepository)
        .searchStudents(furigana, includeDeleted, deletedOnly, "IN_PROGRESS");
    inOrder
        .verify(courseRepository)
        .findCoursesByStudentIds(studentIdsCaptor.capture(), eq(applicationStatus.name()));
    assertThat(studentIdsCaptor.getValue()).containsExactly(studentId);

    inOrder.verify(converter).toDetailDtoList(eq(mockStudents), eq(mockCourses));
  }

  /**
   * getStudentList において、論理削除された受講生のみを検索する場合 （includeDeleted=false, deletedOnly=true）の振る舞いを検証します。
   */
  @Test
  void getStudentList_論理削除のみ検索_で関連メソッドが順に呼ばれ結果が返ること() {

    // 準備
    String furigana = "たかぎ　あかね";
    boolean includeDeleted = false;
    boolean deletedOnly = true;
    ApplicationStatus applicationStatus = ApplicationStatus.IN_PROGRESS;

    UUID studentId = UUID.randomUUID();
    Student s = new Student();
    s.setStudentId(studentId);
    List<Student> mockStudents = List.of(s);

    List<StudentCourse> mockCourses = List.of(new StudentCourse());
    List<StudentDetailDto> expectedDtoList = List.of(new StudentDetailDto());

    when(studentRepository.searchStudents(
            furigana, includeDeleted, deletedOnly, applicationStatus.name()))
        .thenReturn(mockStudents);

    when(courseRepository.findCoursesByStudentIds(anyList(), eq(applicationStatus.name())))
        .thenReturn(mockCourses);

    when(converter.toDetailDtoList(mockStudents, mockCourses)).thenReturn(expectedDtoList);

    // 実行
    List<StudentDetailDto> result =
        service.getStudentList(furigana, includeDeleted, deletedOnly, applicationStatus);

    // 主張
    assertThat(result).isEqualTo(expectedDtoList);

    // 呼び出し検証（順序付き）
    InOrder inOrder = inOrder(studentRepository, courseRepository, converter);
    inOrder
        .verify(studentRepository)
        .searchStudents(furigana, includeDeleted, deletedOnly, applicationStatus.name());
    inOrder
        .verify(courseRepository)
        .findCoursesByStudentIds(studentIdsCaptor.capture(), eq(applicationStatus.name()));
    assertThat(studentIdsCaptor.getValue()).containsExactly(studentId);

    inOrder.verify(converter).toDetailDtoList(eq(mockStudents), eq(mockCourses));
  }

  /**
   * getStudentList において、includeDeleted と deletedOnly を同時に true にした場合、 不正な組み合わせとして {@link
   * IllegalArgumentException} がスローされることを検証します。
   */
  @Test
  void getStudentList_論理削除と削除のみ検索が同時指定された場合_例外がスローされる() {

    // 準備
    String furigana = "たかはし　あや";
    boolean includeDeleted = true;
    boolean deletedOnly = true;
    ApplicationStatus applicationStatus = ApplicationStatus.IN_PROGRESS;

    // 実行
    assertThatThrownBy(
            () -> service.getStudentList(furigana, includeDeleted, deletedOnly, applicationStatus))
        // 検証
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("includeDeletedとdeletedOnlyの両方をtrueにすることはできません");
  }

  /**
   * 申込状況（applicationStatus）を指定した場合に、 {@link
   * raisetech.student.management.repository.StudentRepository#searchStudents(String, boolean,
   * boolean, String)} へ指定したステータスが引数として渡されることを検証します。
   *
   * <p>本テストは「検索条件として status が適切に下層（Repository）へ伝播する」ことに焦点を当てます。 取得した学生一覧に対してコース一覧を取得し、DTOへ変換する一連の流れ
   * （searchAllCourses → converter.toDetailDtoList）も呼ばれることを合わせて確認します。
   *
   * <p>※検索条件を status のみに絞るため、furigana は null を使用します。
   */
  @Test
  void getStudentList_status指定_でsearchStudentsにstatusが渡されること() {
    // 準備
    String furigana = null; // statusだけ見たいなら null でもOK
    boolean includeDeleted = false;
    boolean deletedOnly = false;
    ApplicationStatus applicationStatus = ApplicationStatus.IN_PROGRESS;

    UUID studentId = UUID.randomUUID();
    Student s = new Student();
    s.setStudentId(studentId);
    List<Student> mockStudents = List.of(s);

    List<StudentCourse> mockCourses = List.of(new StudentCourse());
    List<StudentDetailDto> expectedDtoList = List.of(new StudentDetailDto());

    when(studentRepository.searchStudents(
            furigana, includeDeleted, deletedOnly, applicationStatus.name()))
        .thenReturn(mockStudents);

    when(courseRepository.findCoursesByStudentIds(anyList(), eq(applicationStatus.name())))
        .thenReturn(mockCourses);

    when(converter.toDetailDtoList(mockStudents, mockCourses)).thenReturn(expectedDtoList);

    // 実行
    List<StudentDetailDto> result =
        service.getStudentList(furigana, includeDeleted, deletedOnly, applicationStatus);

    // 主張
    assertThat(result).isEqualTo(expectedDtoList);

    InOrder inOrder = inOrder(studentRepository, courseRepository, converter);
    // 呼び出し検証
    inOrder
        .verify(studentRepository)
        .searchStudents(furigana, includeDeleted, deletedOnly, applicationStatus.name());
    inOrder
        .verify(courseRepository)
        .findCoursesByStudentIds(studentIdsCaptor.capture(), eq(applicationStatus.name()));

    assertThat(studentIdsCaptor.getValue()).containsExactly(studentId);
    inOrder.verify(converter).toDetailDtoList(eq(mockStudents), eq(mockCourses));
  }

  /** findStudentById で有効な ID を指定した場合に、対応する受講生情報が取得できることを検証します。 */
  @Test
  void findStudentById_該当する受講生IDで検索した場合_受講生情報が取得できること() {

    // 準備（モックの設定）
    when(studentRepository.findById(studentId)).thenReturn(student);

    // 実行
    Student result = service.findStudentById(studentId);

    // 検証
    assertThat(result).isEqualTo(student);
    verify(studentRepository).findById(studentId);
  }

  /** findStudentById で存在しない ID を指定した場合、 {@link ResourceNotFoundException} がスローされることを検証します。 */
  @Test
  void findStudentById_存在しないIDを指定_ResourceNotFoundExceptionがスローされること() {

    // 準備（モックの設定）
    when(studentRepository.findById(studentId)).thenReturn(null);

    // 実行
    assertThatThrownBy(() -> service.findStudentById(studentId))
        // 検証
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("受講生ID " + UUID_STRING + " が見つかりません。");
  }

  /**
   * {@link StudentServiceImpl#searchCoursesByStudentId(UUID)} で、 受講生IDに紐づくコース一覧が取得できることを検証します。
   *
   * <p>{@link StudentCourseRepository#findCoursesByStudentId(UUID)} への委譲と、
   * リポジトリの戻り値をそのまま返却していることを確認します。
   */
  @Test
  void searchCoursesByStudentId_受講生IDで検索_紐づくコース情報を取得できること() {

    // 準備
    StudentCourse course1 = new StudentCourse();
    course1.setStudentId(studentId);
    course1.setCourseId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001")); // 仮のCourse ID 1

    StudentCourse course2 = new StudentCourse();
    course2.setStudentId(studentId);
    course2.setCourseId(UUID.fromString("123e4567-e89b-12d3-a456-426614174002")); // 仮のCourse ID 2

    List<StudentCourse> expectedCourses = List.of(course1, course2);

    // モックの設定：courseRepositoryがstudentIdで検索されたら、expectedCoursesを返す
    when(courseRepository.findCoursesByStudentId(studentId)).thenReturn(expectedCourses);

    // 実行
    List<StudentCourse> actualCourses = service.searchCoursesByStudentId(studentId);

    // 検証
    assertThat(actualCourses).isEqualTo(expectedCourses);
    verify(courseRepository).findCoursesByStudentId(studentId); // 呼び出されたかも検証
  }

  /** softDeleteStudent で、対象受講生が存在しない場合に {@link ResourceNotFoundException} がスローされることを検証します。 */
  @Test
  void softDeleteStudent_対象受講生が存在しなければ例外メッセージが投げられること() {

    // モックの設定
    when(studentRepository.findById(studentId)).thenReturn(null);

    // 実行
    assertThatThrownBy(() -> service.softDeleteStudent(studentId))
        // 検証
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Student not found for ID: " + UUID_STRING);
  }

  /**
   * softDeleteStudent で、まだ論理削除されていない受講生に対して削除処理が実行されることを検証します。
   *
   * <p>期待する挙動:
   *
   * <ul>
   *   <li>{@code student.softDelete()} が呼ばれる
   *   <li>{@code studentRepository.updateStudent(student)} が呼ばれる
   * </ul>
   */
  @Test
  void softDeleteStudent_論理削除されていなければ削除処理が実行されること() {
    // 準備
    Student student = mock(Student.class);

    // モックの準備
    when(studentRepository.findById(studentId)).thenReturn(student);
    when(student.getDeleted()).thenReturn(false);

    when(studentRepository.updateStudent(student)).thenReturn(1);

    // 実行
    service.softDeleteStudent(studentId);

    // 検証
    verify(student).softDelete();
    verify(studentRepository).updateStudent(student);
  }

  /** restoreStudent で、対象受講生が存在しない場合に {@link ResourceNotFoundException} がスローされることを検証します。 */
  @Test
  void restoreStudent_該当の受講生がいなければ例外メッセージが投げられること() {

    // モックの設定
    when(studentRepository.findById(studentId)).thenReturn(null);

    // 実行
    Throwable thrown = catchThrowable(() -> service.restoreStudent(studentId));
    // 検証
    assertThat(thrown)
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("受講生ID " + UUID_STRING + " が見つかりません。");
  }

  /** restoreStudent で、論理削除されている受講生が存在する場合に 復元処理と更新処理が実行されることを検証します。 */
  @Test
  void restoreStudent_論理削除されている受講生が存在すれば受講生情報を復元すること() {
    Student student = mock(Student.class);

    when(student.getDeleted()).thenReturn(true);
    when(studentRepository.findById(studentId)).thenReturn(student);
    when(studentRepository.updateStudent(student)).thenReturn(1);

    service.restoreStudent(studentId);

    verify(student).restore(); // restore() 呼び出しの検証（可能なら）
    verify(studentRepository).updateStudent(student);
  }

  /** restoreStudent で、論理削除されていない受講生に対しては 更新処理が行われないことを検証します。 */
  @Test
  void restoreStudent_論理削除されていない場合は更新処理が行われないこと() {
    Student student = mock(Student.class);

    when(student.getDeleted()).thenReturn(false);
    when(studentRepository.findById(studentId)).thenReturn(student);

    service.restoreStudent(studentId);

    verify(studentRepository, never()).updateStudent(any());
  }

  /**
   * forceDeleteStudent 実行時に、物理削除の件数が 0 件だった場合、 対象受講生が存在しないものとみなして ResourceNotFoundException
   * を送出することを検証します。
   *
   * <p>このとき先に実行した deleteCoursesByStudentId の削除も {@code @Transactional} によりロールバックされる前提であり、
   * 部分的な削除状態が残らないことを保証します（実装側の意図の確認）。
   */
  @Test
  void forceDeleteStudent_該当の受講生が存在しない時は例外がスローされること() {
    // モックの設定
    when(studentRepository.forceDeleteStudent(studentId)).thenReturn(0);

    // 実行＆検証
    assertThatThrownBy(() -> service.forceDeleteStudent(studentId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("受講生ID " + UUID_STRING + " が見つかりません。");

    verify(courseRepository).deleteCoursesByStudentId(studentId);
    verify(studentRepository).forceDeleteStudent(studentId);
  }

  /**
   * forceDeleteStudent で、受講生が存在する場合に 紐づくコースおよび受講生レコードが順序通り削除されることを検証します。
   *
   * <p>期待する呼び出し順:
   *
   * <ol>
   *   <li>{@code courseRepository.deleteCoursesByStudentId(studentId)}
   *   <li>{@code studentRepository.forceDeleteStudent(studentId)}
   * </ol>
   */
  @Test
  void forceDeleteStudent_受講生が存在する場合はコースと受講生情報が削除されること() {
    // モックの準備
    when(studentRepository.forceDeleteStudent(studentId)).thenReturn(1);

    // 実行
    service.forceDeleteStudent(studentId);

    // 検証：コース削除と物理削除が順序通り呼び出されたか
    InOrder inOrder = inOrder(courseRepository, studentRepository);

    inOrder.verify(courseRepository).deleteCoursesByStudentId(studentId);
    inOrder.verify(studentRepository).forceDeleteStudent(studentId);
    // 想定外の呼び出しがないか検証（すでに渡したmockを対象に）
    inOrder.verifyNoMoreInteractions();
  }
}
