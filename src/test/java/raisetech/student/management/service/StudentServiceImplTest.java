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

import java.util.Base64;
import java.util.List;

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

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

  @Mock private StudentRepository studentRepository;

  @Mock private StudentCourseRepository courseRepository;

  @Mock private StudentConverter converter;

  @InjectMocks private StudentServiceImpl service;

  private static final String BASE64_ID = "3Jz8kUv2Rgq7Y+DnX3+aRQ==";

  private byte[] studentId;
  private Student student;
  private StudentServiceImpl spyService;

  // 各種テスト共通オブジェクトを準備
  @BeforeEach
  void setUp() {
    studentId = Base64.getDecoder().decode(BASE64_ID);

    student = new Student();
    student.setStudentId(studentId);
    student.setFullName("テスト　花子");
    student.setEmail("test@example.com");
    student.setAge(30);
    spyService = Mockito.spy(service);

    // コースを1件追加してcoursesを初期化
    StudentCourse course = new StudentCourse();
    course.setStudentId(studentId);
    course.setCourseId(new byte[16]); // 任意のダミーUUID（16バイト）
  }

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

  @Test
  void updateStudent_受講生情報更新時_既存コースが削除され_新規コースが登録されること() {

    // updateStudent用のオブジェクトを準備
    StudentCourse course = new StudentCourse();
    course.setStudentId(student.getStudentId());
    course.setCourseId(Base64.getDecoder().decode("aGVsbG8tY291cnNlLWlk")); // 仮のCourse ID

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

  @Test
  void partialUpdateStudent_部分更新時_受講生情報が更新され_既存コース削除後_新規コースが登録されること() {

    // partialUpdateStudent用のオブジェクトを準備
    Student student = new Student();
    student.setStudentId(Base64.getDecoder().decode("3Jz8kUv2Rgq7Y+DnX3+aRQ=="));
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

  @Test
  void findStudentById_存在しないIDを指定_ResourceNotFoundExceptionがスローされること() {

    // 準備（モックの設定）
    when(studentRepository.findById(studentId)).thenReturn(null);
    when(converter.encodeBase64(studentId)).thenReturn(BASE64_ID);

    // 実行
    assertThatThrownBy(() -> service.findStudentById(studentId))
        // 検証
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("受講生ID " + BASE64_ID + " が見つかりません。");
  }

  @Test
  void searchCoursesByStudentId_受講生IDで検索_紐づくコース情報を取得できること() {

    // 準備
    StudentCourse course1 = new StudentCourse();
    course1.setStudentId(studentId);
    course1.setCourseId(Base64.getDecoder().decode("Y291cnNlLWlkMQ==")); // 仮のCourse ID 1

    StudentCourse course2 = new StudentCourse();
    course2.setStudentId(studentId);
    course2.setCourseId(Base64.getDecoder().decode("Y291cnNlLWlkMg==")); // 仮のCourse ID 2

    List<StudentCourse> expectedCourses = List.of(course1, course2);

    // モックの設定：courseRepositoryがstudentIdで検索されたら、expectedCoursesを返す
    when(courseRepository.findCoursesByStudentId(studentId)).thenReturn(expectedCourses);

    // 実行
    List<StudentCourse> actualCourses = service.searchCoursesByStudentId(studentId);

    // 検証
    assertThat(actualCourses).isEqualTo(expectedCourses);
    verify(courseRepository).findCoursesByStudentId(studentId); // 呼び出されたかも検証
  }

  @Test
  void searchAllCourses_コース情報を全件取得すること() {

    // 準備
    StudentCourse course1 = new StudentCourse();
    course1.setCourseId(Base64.getDecoder().decode("Y291cnNlLWlkMw=="));
    course1.setCourseName("Javaコース");

    StudentCourse course2 = new StudentCourse();
    course2.setCourseId(Base64.getDecoder().decode("Y291cnNlLWlkNA=="));
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

  @Test
  void softDeleteStudent_対象受講生が存在しなければ例外メッセージが投げられること() {

    // モックの設定
    when(studentRepository.findById(studentId)).thenReturn(null);
    when(converter.encodeBase64(studentId)).thenReturn(BASE64_ID);

    // 実行
    assertThatThrownBy(() -> spyService.softDeleteStudent(studentId))
        // 検証
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Student not found for ID: " + BASE64_ID);
  }

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

  @Test
  void restoreStudent_該当の受講生がいなければ例外メッセージが投げられること() {

    // モックの設定
    when(studentRepository.findById(studentId)).thenReturn(null);
    when(converter.encodeBase64(studentId)).thenReturn(BASE64_ID);

    // 実行
    Throwable thrown = catchThrowable(() -> service.restoreStudent(studentId));
    // 検証
    assertThat(thrown)
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("受講生ID " + BASE64_ID + " が見つかりません。");
  }

  @Test
  void restoreStudent_論理削除されている受講生が存在すれば受講生情報を復元すること() {
    Student student = mock(Student.class);

    when(student.getDeleted()).thenReturn(true);
    when(studentRepository.findById(studentId)).thenReturn(student);
    when(converter.encodeBase64(studentId)).thenReturn(BASE64_ID);

    service.restoreStudent(studentId);

    verify(student).restore(); // restore() 呼び出しの検証（可能なら）
    verify(studentRepository).updateStudent(student);
  }

  @Test
  void restoreStudent_論理削除されていない場合は更新処理が行われないこと() {
    Student student = mock(Student.class);

    when(student.getDeleted()).thenReturn(false);
    when(studentRepository.findById(studentId)).thenReturn(student);
    when(converter.encodeBase64(studentId)).thenReturn(BASE64_ID);

    service.restoreStudent(studentId);

    verify(studentRepository, never()).updateStudent(any());
  }

  @Test
  void forceDeleteStudent_該当の受講生が存在しない時は例外がスローされること() {
    // モックの設定
    when(studentRepository.findById(studentId)).thenReturn(null);
    when(converter.encodeBase64(studentId)).thenReturn(BASE64_ID);

    // 実行＆検証
    assertThatThrownBy(() -> service.forceDeleteStudent(studentId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("受講生ID " + BASE64_ID + " が見つかりません。");
  }

  @Test
  void forceDeleteStudent_受講生が存在する場合はコースと受講生情報が削除されること() {

    // 準備
    Student student = mock(Student.class);

    // モックの準備
    when(studentRepository.findById(studentId)).thenReturn(student);
    when(converter.encodeBase64(studentId)).thenReturn(BASE64_ID);

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
