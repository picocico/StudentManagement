package raisetech.student.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.exception.ResourceNotFoundException;
import raisetech.student.management.repository.StudentCourseRepository;
import raisetech.student.management.repository.StudentRepository;
import raisetech.student.management.util.UUIDUtil;

/**
 * {@link StudentServiceImpl} の単体テストクラス。
 *
 * <p>UUID を byte[16]（BINARY(16)）として扱うドメイン設計を前提に、
 * 受講生情報およびコース情報に対するサービス層の振る舞いを検証します。
 *
 * <ul>
 *   <li>リポジトリ層への委譲が正しく行われているか</li>
 *   <li>コースの更新・追加ロジックが意図通りに呼び出されるか</li>
 *   <li>論理削除・復元・物理削除における例外処理/メッセージが期待通りか</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

  /**
   * 受講生リポジトリのモック。
   *
   * <p>受講生情報の永続化や検索の振る舞いをスタブ/検証するために利用します。
   */
  @Mock
  private StudentRepository studentRepository;

  /**
   * 受講生コースリポジトリのモック。
   *
   * <p>コース情報の取得・登録・削除などをスタブ/検証するために利用します。
   */
  @Mock
  private StudentCourseRepository courseRepository;

  /**
   * エンティティとDTO間の変換を行うコンバーターのモック。
   *
   * <p>サービス層からの呼び出しを検証しつつ、DTOリスト生成などをスタブします。
   */
  @Mock
  private StudentConverter converter;

  /**
   * テスト対象のサービス実装。
   *
   * <p>{@link InjectMocks} により、上記モックがインジェクションされた状態の
   * {@link StudentServiceImpl} が生成されます。
   */
  @InjectMocks
  private StudentServiceImpl service;

  /**
   * テストで利用する固定 UUID 文字列表現。
   *
   * <p>この値を {@link UUIDUtil#toBytes(UUID)} で byte[16] に変換して
   * {@code studentId} として利用します。
   */
  private static final String UUID_STRING = "123e4567-e89b-12d3-a456-426614174000";

  /**
   * テスト共通で使用する受講生 ID（UUID を BINARY(16) にした 16バイト配列）。
   */
  private byte[] studentId;

  /**
   * テスト共通で使用する受講生エンティティ。
   */
  private Student student;

  /**
   * searchAllCourses など、一部メソッドを差し替えて振る舞いを検証するためのスパイ。
   */
  private StudentServiceImpl spyService;


  /**
   * 各テスト実行前に共通の準備を行います。
   *
   * <ul>
   *   <li>{@link #UUID_STRING} から {@link #studentId} を生成</li>
   *   <li>{@link #student} の基本情報を初期化</li>
   *   <li>{@link #spyService} を作成</li>
   *   <li>必要に応じたダミーの {@link StudentCourse} を生成</li>
   * </ul>
   */
  @BeforeEach
  void setUp() {
    // UUID文字列 → UUID → byte[16]
    studentId = UUIDUtil.toBytes(UUID.fromString(UUID_STRING));

    student = new Student();
    student.setStudentId(studentId);
    student.setFullName("テスト　花子");
    student.setEmail("test@example.com");
    student.setAge(30);

    spyService = Mockito.spy(service);

    // コースを1件追加してcoursesを初期化（必要に応じて各テスト内で利用）
    StudentCourse course = new StudentCourse();
    course.setStudentId(studentId);
    course.setCourseId(new byte[16]); // 任意のダミーUUID（16バイト）
  }

  /**
   * 受講生登録時に、コースリストが空であればコース登録が行われないことを検証します。
   *
   * <p>期待する挙動:
   * <ul>
   *   <li>{@code insertStudent(student)} は 1 回呼ばれる</li>
   *   <li>{@code insertCourses(...)} は 1 度も呼ばれない</li>
   * </ul>
   */
  @Test
  void 受講生登録時_コースが空ならコースは登録されないこと() {
    // コースが空のケースにする
    List<StudentCourse> emptyCourses = List.of();
    // 実行
    service.registerStudent(student, emptyCourses);

    // 検証
    // insertStudent() が1回呼ばれているかどうか？
    verify(studentRepository, times(1)).insertStudent(student);
    // insertCourses() は一度も呼ばれなかったかどうか？
    verify(courseRepository, never()).insertCourses(anyList());
  }

  /**
   * 受講生情報の全体更新時に、既存のコースが削除され、新しいコースが登録されることを検証します。
   *
   * <p>期待する呼び出し順:
   * <ol>
   *   <li>{@code studentRepository.updateStudent(student)}</li>
   *   <li>{@code courseRepository.deleteCoursesByStudentId(studentId)}</li>
   *   <li>{@code courseRepository.insertCourses(courses)}</li>
   * </ol>
   */
  @Test
  void updateStudent_受講生情報更新時_既存コースが削除され_新規コースが登録されること() {

    // updateStudent用のオブジェクトを準備
    StudentCourse course = new StudentCourse();
    course.setStudentId(student.getStudentId());
    course.setCourseId(new byte[16]); // 仮のCourse ID

    List<StudentCourse> courses = List.of(course);

    // 実行
    service.updateStudent(student, courses);

    // 検証 メソッドの呼び出しが順序通り行えているか？
    InOrder inOrder = inOrder(studentRepository, courseRepository);
    // studentRepository.updateStudent(student)メソッドが呼び出されているかどうか？
    inOrder.verify(studentRepository).updateStudent(student);
    // studentId に紐づく受講コースを 一括削除する処理が実行されたかどうか？
    inOrder.verify(courseRepository).deleteCoursesByStudentId(student.getStudentId());
    // 新たにcoursesをDBに登録する処理が呼び出されたかどうか？
    inOrder.verify(courseRepository).insertCourses(courses);
  }

  /**
   * 部分更新（partialUpdateStudent）時に、受講生情報が更新され、 既存コース削除 → 新規コース登録が行われることを検証します。
   *
   * <p>期待する呼び出し順:
   * <ol>
   *   <li>{@code studentRepository.updateStudent(student)}</li>
   *   <li>{@code courseRepository.deleteCoursesByStudentId(studentId)}</li>
   *   <li>{@code courseRepository.insertCourses(courses)}</li>
   * </ol>
   */
  @Test
  void partialUpdateStudent_部分更新時_受講生情報が更新され_既存コース削除後_新規コースが登録されること() {

    // partialUpdateStudent用のオブジェクトを準備
    Student student = new Student();
    student.setStudentId(studentId);
    student.setFullName("検証 太郎");

    List<StudentCourse> courses = List.of(new StudentCourse());

    // 実行
    service.partialUpdateStudent(student, courses);

    // 検証
    InOrder inOrder = inOrder(studentRepository, courseRepository);
    // studentRepository.updateStudent(student)メソッドが呼び出されているかどうか？
    inOrder.verify(studentRepository).updateStudent(student);
    // studentId に紐づく受講コースを 一括削除する処理が実行されたかどうか？
    inOrder.verify(courseRepository).deleteCoursesByStudentId(student.getStudentId());
    inOrder.verify(courseRepository).insertCourses(courses);
  }

  /**
   * appendCourses 実行時に、既に存在するコースには insert されないことを検証します。
   *
   * <p>サービスでは {@code courseRepository.insertIfNotExists(...)} を呼び出し、
   * リポジトリ側で重複チェックを行う前提です。
   */
  @Test
  void appendCourses_既に存在するコースにはinsertされないこと() {

    // appendCourses用のオブジェクトを準備
    StudentCourse course = new StudentCourse();
    course.setStudentId(student.getStudentId());
    course.setCourseId(new byte[16]);

    List<StudentCourse> newCourses = List.of(course);

    // 実行
    service.appendCourses(student.getStudentId(), newCourses);

    // insertIfNotExists が正しく呼ばれているか?
    verify(courseRepository).insertIfNotExists(course);
    // コースが複数ある場合は、すべて処理されているか?
    verify(courseRepository, times(newCourses.size())).insertIfNotExists(any());
  }

  /**
   * updateStudentInfoOnly が呼び出された際に、リポジトリへ委譲されることを検証します。
   *
   * <p>期待する挙動: {@code studentRepository.updateStudent(student)} が 1回呼ばれる。
   */
  @Test
  void updateStudentInfoOnlyが呼び出されると_リポジトリに委譲されること() {

    // 準備
    Student student = new Student();
    student.setStudentId(new byte[16]);
    student.setFullName("検証　テスト");

    // 実行
    service.updateStudentInfoOnly(student);

    // 検証：studentRepository.updateStudent(...) が呼ばれていること
    verify(studentRepository).updateStudent(student);
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

    List<Student> mockStudents = List.of(new Student());
    List<StudentCourse> mockCourses = List.of(new StudentCourse());
    List<StudentDetailDto> expectedDtoList = List.of(new StudentDetailDto());

    when(studentRepository.searchStudents(furigana, includeDeleted, deletedOnly))
        .thenReturn(mockStudents);

    // searchAllCourses() の戻り値を差し替え
    doReturn(mockCourses).when(spyService).searchAllCourses();

    when(converter.toDetailDtoList(mockStudents, mockCourses)).thenReturn(expectedDtoList);

    // 実行
    List<StudentDetailDto> result =
        spyService.getStudentList(furigana, includeDeleted, deletedOnly);

    // 主張
    assertThat(result).isEqualTo(expectedDtoList);

    // 呼び出し検証（順序付き）
    InOrder inOrder = inOrder(studentRepository, spyService, converter);
    inOrder.verify(studentRepository).searchStudents(furigana, includeDeleted, deletedOnly);
    inOrder.verify(spyService).searchAllCourses();
    inOrder.verify(converter).toDetailDtoList(mockStudents, mockCourses);
  }

  /**
   * getStudentList において、論理削除を含めて検索する場合 （includeDeleted=true, deletedOnly=false）の振る舞いを検証します。
   */
  @Test
  void getStudentList_論理削除含めた検索_で関連メソッドが順に呼ばれ結果が返ること() {

    // 準備
    String furigana = "さとう　じろう";
    boolean includeDeleted = true;
    boolean deletedOnly = false;

    List<Student> mockStudents = List.of(new Student());
    List<StudentCourse> mockCourses = List.of(new StudentCourse());
    List<StudentDetailDto> expectedDtoList = List.of(new StudentDetailDto());

    when(studentRepository.searchStudents(furigana, includeDeleted, deletedOnly))
        .thenReturn(mockStudents);

    // searchAllCourses() の戻り値を差し替え
    doReturn(mockCourses).when(spyService).searchAllCourses();

    when(converter.toDetailDtoList(mockStudents, mockCourses)).thenReturn(expectedDtoList);

    // 実行
    List<StudentDetailDto> result =
        spyService.getStudentList(furigana, includeDeleted, deletedOnly);

    // 主張
    assertThat(result).isEqualTo(expectedDtoList);

    // 呼び出し検証（順序付き）
    InOrder inOrder = inOrder(studentRepository, spyService, converter);
    inOrder.verify(studentRepository).searchStudents(furigana, includeDeleted, deletedOnly);
    inOrder.verify(spyService).searchAllCourses();
    inOrder.verify(converter).toDetailDtoList(mockStudents, mockCourses);
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

    List<Student> mockStudents = List.of(new Student());
    List<StudentCourse> mockCourses = List.of(new StudentCourse());
    List<StudentDetailDto> expectedDtoList = List.of(new StudentDetailDto());

    when(studentRepository.searchStudents(furigana, includeDeleted, deletedOnly))
        .thenReturn(mockStudents);

    // searchAllCourses() の戻り値を差し替え
    doReturn(mockCourses).when(spyService).searchAllCourses();

    when(converter.toDetailDtoList(mockStudents, mockCourses)).thenReturn(expectedDtoList);

    // 実行
    List<StudentDetailDto> result =
        spyService.getStudentList(furigana, includeDeleted, deletedOnly);

    // 主張
    assertThat(result).isEqualTo(expectedDtoList);

    // 呼び出し検証（順序付き）
    InOrder inOrder = inOrder(studentRepository, spyService, converter);
    inOrder.verify(studentRepository).searchStudents(furigana, includeDeleted, deletedOnly);
    inOrder.verify(spyService).searchAllCourses();
    inOrder.verify(converter).toDetailDtoList(mockStudents, mockCourses);
  }

  /**
   * getStudentList において、includeDeleted と deletedOnly を同時に true にした場合、 不正な組み合わせとして
   * {@link IllegalArgumentException} がスローされることを検証します。
   */
  @Test
  void getStudentList_論理削除と削除のみ検索が同時指定された場合_例外がスローされる() {

    // 準備
    String furigana = "たかはし　あや";
    boolean includeDeleted = true;
    boolean deletedOnly = true;

    // 実行
    assertThatThrownBy(() -> spyService.getStudentList(furigana, includeDeleted, deletedOnly))
        // 検証
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("includeDeletedとdeletedOnlyの両方をtrueにすることはできません");
  }

  /**
   * findStudentById で有効な ID を指定した場合に、対応する受講生情報が取得できることを検証します。
   */
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

  /**
   * findStudentById で存在しない ID を指定した場合、 {@link ResourceNotFoundException} がスローされることを検証します。
   */
  @Test
  void findStudentById_存在しないIDを指定_ResourceNotFoundExceptionがスローされること() {

    // 準備（モックの設定）
    when(studentRepository.findById(studentId)).thenReturn(null);
    when(converter.encodeUuidString(studentId)).thenReturn(UUID_STRING);

    // 実行
    assertThatThrownBy(() -> service.findStudentById(studentId))
        // 検証
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("受講生ID " + UUID_STRING + " が見つかりません。");
  }

  /**
   * searchCoursesByStudentId で、受講生 ID に紐づくコース一覧が取得できることを検証します。
   *
   * <p>{@link StudentCourseRepository#findCoursesByStudentId(byte[])} の委譲と
   * 戻り値のそのまま返却を確認します。
   */
  @Test
  void searchCoursesByStudentId_受講生IDで検索_紐づくコース情報を取得できること() {

    // 準備
    StudentCourse course1 = new StudentCourse();
    course1.setStudentId(studentId);
    course1.setCourseId(UUIDUtil.toBytes(
        UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))); // 仮のCourse ID 1

    StudentCourse course2 = new StudentCourse();
    course2.setStudentId(studentId);
    course2.setCourseId(UUIDUtil.toBytes(
        UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))); // 仮のCourse ID 2

    List<StudentCourse> expectedCourses = List.of(course1, course2);

    // モックの設定：courseRepositoryがstudentIdで検索されたら、expectedCoursesを返す
    when(courseRepository.findCoursesByStudentId(studentId)).thenReturn(expectedCourses);

    // 実行
    List<StudentCourse> actualCourses = service.searchCoursesByStudentId(studentId);

    // 検証
    assertThat(actualCourses).isEqualTo(expectedCourses);
    verify(courseRepository).findCoursesByStudentId(studentId); // 呼び出されたかも検証
  }

  /**
   * searchAllCourses で、コース情報が全件取得できることを検証します。
   *
   * <p>{@link StudentCourseRepository#findAllCourses()} の結果をそのまま返していることを確認します。
   */
  @Test
  void searchAllCourses_コース情報を全件取得すること() {

    // 準備
    StudentCourse course1 = new StudentCourse();
    course1.setCourseId(UUIDUtil.toBytes(UUID.fromString("123e4567-e89b-12d3-a456-426614174003")));
    course1.setCourseName("Javaコース");

    StudentCourse course2 = new StudentCourse();
    course2.setCourseId(UUIDUtil.toBytes(UUID.fromString("123e4567-e89b-12d3-a456-426614174003")));
    course2.setCourseName("AWSコース");

    List<StudentCourse> mockCourses = List.of(course1, course2);

    // モック設定
    when(courseRepository.findAllCourses()).thenReturn(mockCourses);

    // 実行
    List<StudentCourse> result = service.searchAllCourses();

    // 検証
    assertThat(result).isEqualTo(mockCourses);
    verify(courseRepository).findAllCourses(); // 呼び出しがされたかどうかの確認
  }

  /**
   * softDeleteStudent で、対象受講生が存在しない場合に {@link ResourceNotFoundException} がスローされることを検証します。
   */
  @Test
  void softDeleteStudent_対象受講生が存在しなければ例外メッセージが投げられること() {

    // モックの設定
    when(studentRepository.findById(studentId)).thenReturn(null);
    when(converter.encodeUuidString(studentId)).thenReturn(UUID_STRING);

    // 実行
    assertThatThrownBy(() -> spyService.softDeleteStudent(studentId))
        // 検証
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Student not found for ID: " + UUID_STRING);
  }

  /**
   * softDeleteStudent で、まだ論理削除されていない受講生に対して削除処理が実行されることを検証します。
   *
   * <p>期待する挙動:
   * <ul>
   *   <li>{@code student.softDelete()} が呼ばれる</li>
   *   <li>{@code studentRepository.updateStudent(student)} が呼ばれる</li>
   * </ul>
   */
  @Test
  void softDeleteStudent_論理削除されていなければ削除処理が実行されること() {

    // 準備
    Student student = mock(Student.class);

    // モックの準備
    when(studentRepository.findById(studentId)).thenReturn(student);
    when(student.getDeleted()).thenReturn(false);

    // 実行
    service.softDeleteStudent(studentId);

    // 検証
    verify(student).softDelete();
    verify(studentRepository).updateStudent(student);
  }

  /**
   * restoreStudent で、対象受講生が存在しない場合に {@link ResourceNotFoundException} がスローされることを検証します。
   */
  @Test
  void restoreStudent_該当の受講生がいなければ例外メッセージが投げられること() {

    // モックの設定
    when(studentRepository.findById(studentId)).thenReturn(null);
    when(converter.encodeUuidString(studentId)).thenReturn(UUID_STRING);

    // 実行
    Throwable thrown = catchThrowable(() -> service.restoreStudent(studentId));
    // 検証
    assertThat(thrown)
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("受講生ID " + UUID_STRING + " が見つかりません。");
  }

  /**
   * restoreStudent で、論理削除されている受講生が存在する場合に 復元処理と更新処理が実行されることを検証します。
   */
  @Test
  void restoreStudent_論理削除されている受講生が存在すれば受講生情報を復元すること() {
    Student student = mock(Student.class);

    when(student.getDeleted()).thenReturn(true);
    when(studentRepository.findById(studentId)).thenReturn(student);
    when(converter.encodeUuidString(studentId)).thenReturn(UUID_STRING);

    service.restoreStudent(studentId);

    verify(student).restore(); // restore() 呼び出しの検証（可能なら）
    verify(studentRepository).updateStudent(student);
  }

  /**
   * restoreStudent で、論理削除されていない受講生に対しては 更新処理が行われないことを検証します。
   */
  @Test
  void restoreStudent_論理削除されていない場合は更新処理が行われないこと() {
    Student student = mock(Student.class);

    when(student.getDeleted()).thenReturn(false);
    when(studentRepository.findById(studentId)).thenReturn(student);
    when(converter.encodeUuidString(studentId)).thenReturn(UUID_STRING);

    service.restoreStudent(studentId);

    verify(studentRepository, never()).updateStudent(any());
  }

  /**
   * forceDeleteStudent で、該当受講生が存在しない場合に {@link ResourceNotFoundException} がスローされることを検証します。
   */
  @Test
  void forceDeleteStudent_該当の受講生が存在しない時は例外がスローされること() {
    // モックの設定
    when(studentRepository.findById(studentId)).thenReturn(null);
    when(converter.encodeUuidString(studentId)).thenReturn(UUID_STRING);

    // 実行＆検証
    assertThatThrownBy(() -> service.forceDeleteStudent(studentId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("受講生ID " + UUID_STRING + " が見つかりません。");
  }

  /**
   * forceDeleteStudent で、受講生が存在する場合に 紐づくコースおよび受講生レコードが順序通り削除されることを検証します。
   *
   * <p>期待する呼び出し順:
   * <ol>
   *   <li>{@code courseRepository.deleteCoursesByStudentId(studentId)}</li>
   *   <li>{@code studentRepository.forceDeleteStudent(studentId)}</li>
   * </ol>
   */
  @Test
  void forceDeleteStudent_受講生が存在する場合はコースと受講生情報が削除されること() {

    // 準備
    Student student = mock(Student.class);

    // モックの準備
    when(studentRepository.findById(studentId)).thenReturn(student);
    when(converter.encodeUuidString(studentId)).thenReturn(UUID_STRING);

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
