package raisetech.student.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentCourseDto;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentDto;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.exception.ResourceNotFoundException;
import raisetech.student.management.repository.StudentCourseApplicationStatusRepository;
import raisetech.student.management.repository.StudentCourseRepository;
import raisetech.student.management.repository.StudentRepository;

/**
 * StudentServiceImpl#patchStudent の単体テスト。
 *
 * <p>前提:
 *
 * <ul>
 *   <li>Controller 層で raw body 判定（NONE/EMPTY_OBJECT 等）および isPatchEmpty() による E003 判定は完了している
 *   <li>本テストでは service 層の「student/courses の更新ロジック」および Repository 呼び出しの有無・回数・引数を検証する
 *   <li>DTO バリデーション（@NotBlank 等）は Controller 層の責務のため、本テストでは扱わない
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class StudentServiceImplPatchStudentTest {

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
   * 受講生コースの申請状況のリポジトリのモック。
   *
   * <p>コースの申請状況の取得・登録・削除などをスタブ/検証するために利用します。
   */
  @Mock
  private StudentCourseApplicationStatusRepository statusRepository;

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
   * <p>{@link InjectMocks} により、上記モックがインジェクションされた状態の {@link StudentServiceImpl} が生成されます。
   */
  @InjectMocks
  private StudentServiceImpl service;

  /**
   * テスト共通で使用する受講生 ID（UUID）。
   */
  private UUID studentId;

  /**
   * テスト共通で使用する受講生 ID（文字列）。
   */
  private String studentIdString;

  /**
   * テスト共通で使用する既存受講生エンティティ。
   */
  private Student existingStudent;

  /**
   * テスト共通で使用する期待結果の受講生詳細DTO（戻り値）。
   */
  private StudentDetailDto expectedDetail;

  // patchStudent 内で findStudentById(studentId) が複数回呼ばれるため、
  // 毎回同じ existingStudent を返すようにしておく
  @BeforeEach
  void setUp() {
    studentId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    studentIdString = studentId.toString();

    existingStudent = new Student();
    existingStudent.setStudentId(studentId);

    expectedDetail = mock(StudentDetailDto.class);

    // findStudentById(studentId) が内部で2回呼ばれる実装なので、
    // 毎回 existingStudent を返すようにしておく
    when(studentRepository.findById(studentId)).thenReturn(existingStudent);

    // loadLatestDetail の変換を固定
    lenient().when(courseRepository.findCoursesByStudentId(studentId)).thenReturn(List.of());
    lenient().
        when(converter.toDetailDto(any(Student.class), anyList(), eq(studentIdString)))
        .thenReturn(expectedDetail);
  }

  // ----------------------------------------------------------------------
  // 1) courses == null → コース更新はしない + findCoursesByStudentId は呼ぶ
  // ----------------------------------------------------------------------
  @Test
  void patchStudent_coursesがnullならコースは触らないこと() {
    // Arrange
    StudentDto sDto = new StudentDto();
    sDto.setFullName("name"); // studentの更新をありにする
    StudentRegistrationRequest req = new StudentRegistrationRequest();
    req.setStudent(sDto);
    req.setCourses(null); // ★コース未指定

    // converter.toEntity(studentDto) と mergeStudent が呼ばれる前提
    Student updateEntity = new Student();
    when(converter.toEntity(any(StudentDto.class))).thenReturn(updateEntity);

    // Act
    StudentDetailDto result = service.patchStudent(studentId, req, studentIdString);

    // Assert
    assertThat(result).isSameAs(expectedDetail);

    // student の selective update が呼ばれる
    verify(studentRepository).updateStudentSelective(existingStudent);

    // courses が null なので courseRepository へは「更新系」は一切行かない
    verify(courseRepository, never()).deleteCoursesByStudentId(any());
    verify(courseRepository, never()).insertCourses(anyList());
    verify(courseRepository, never()).updateCourseSelective(any());
    verify(courseRepository, never()).deleteCoursesByCourseIds(any(), anyList());

    // status も触らない
    verifyNoInteractions(statusRepository);
  }

  // --------------------------------------------------
  // 2) courses=[] & append=true(省略true) → no-op
  // --------------------------------------------------
  @Test
  void patchStudent_courses空配列_append省略trueならnoOpでコースは触らないこと() {
    // Arrange
    StudentRegistrationRequest req = new StudentRegistrationRequest();
    req.setStudent(null); // studentの更新はなし
    req.setCourses(List.of()); // ★空配列
    req.setAppendCourses(null); // ★省略 = true 扱い

    // Act
    StudentDetailDto result = service.patchStudent(studentId, req, studentIdString);

    // Assert
    assertThat(result).isSameAs(expectedDetail);

    verify(courseRepository, never()).deleteCoursesByStudentId(any());
    verify(courseRepository, never()).insertCourses(anyList());
    verify(courseRepository, never()).updateCourseSelective(any());
    verify(courseRepository, never()).deleteCoursesByCourseIds(any(), anyList());
    verifyNoInteractions(statusRepository);

    // student が null なので studentRepository.updateStudentSelective も呼ばれない
    verify(studentRepository, times(2)).findById(studentId);
    verify(studentRepository, never()).updateStudentSelective(any());
  }

  // --------------------------------------------------------------
  // 3) courses=null → 「フィールド未指定」扱いでコースは触らない（no touch）
  // --------------------------------------------------------------
  @Test
  void patchStudent_coursesがnullであれば_既存のcoursesの状態を変更しないこと() {

    StudentRegistrationRequest req = new StudentRegistrationRequest();
    req.setStudent(null);
    req.setAppendCourses(false); // ここがfalseでも、courses=nullなら触らないのが期待
    req.setCourses(null);

    StudentDetailDto result = service.patchStudent(studentId, req, studentIdString);
    assertThat(result).isSameAs(expectedDetail);

    verify(courseRepository, never()).deleteCoursesByStudentId(any());
    verify(courseRepository, never()).deleteCoursesByCourseIds(any(), anyList());
    verify(courseRepository, never()).insertCourses(anyList());
    verify(courseRepository, never()).updateCourseSelective(any());

    verifyNoInteractions(statusRepository);
    // student=null なので student 更新もしない
    verify(studentRepository, never()).updateStudentSelective(any());

  }

  // ------------------------------------------
  // 4) courses=[] & append=false → 全削除
  // ------------------------------------------
  @Test
  void patchStudent_courses空配列_append_falseなら全削除されること() {
    // Arrange
    StudentRegistrationRequest req = new StudentRegistrationRequest();
    req.setStudent(null);
    req.setCourses(List.of()); // ★空配列
    req.setAppendCourses(false); // ★差し替え = 全削除

    // Act
    service.patchStudent(studentId, req, studentIdString);

    // Assert
    verify(courseRepository).deleteCoursesByStudentId(studentId);
    verify(courseRepository, never()).insertCourses(anyList());
    verify(courseRepository, never()).updateCourseSelective(any());
    verifyNoInteractions(statusRepository);
  }

  // -----------------------------------------------------------
  // 5) append=false 差し替え　→　requestに無い courseId を削除
  // -----------------------------------------------------------
  @Test
  void patchStudent_appendFalse差し替えで_existingからrequestにないcourseIdが削除されること() {
    // Arrange
    UUID keepId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    UUID deleteId = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");

    // 既存コース: keepId, deleteId
    StudentCourse e1 = new StudentCourse();
    e1.setCourseId(keepId);
    StudentCourse e2 = new StudentCourse();
    e2.setCourseId(deleteId);
    when(courseRepository.findCoursesByStudentId(studentId)).thenReturn(List.of(e1, e2));

    // リクエストは keepId のみ（= deleteId は消えるはず）
    StudentCourseDto dto = new StudentCourseDto();
    dto.setCourseId(keepId.toString());
    dto.setCourseName("Java"); // courseFieldsありにして update 側に流す

    StudentRegistrationRequest req = new StudentRegistrationRequest();
    req.setStudent(null);
    req.setCourses(List.of(dto));
    req.setAppendCourses(false); // ★差し替え

    StudentCourse toUpdate = new StudentCourse();
    toUpdate.setCourseId(keepId);
    toUpdate.setCourseName("Java");
    when(converter.toCourseEntities(eq(studentId), anyList())).thenReturn(List.of(toUpdate));

    when(courseRepository.updateCourseSelective(any(StudentCourse.class))).thenReturn(1);

    // Act
    service.patchStudent(studentId, req, studentIdString);

    // Assert: deleteId だけ削除対象に渡される
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<UUID>> deleteIdsCaptor = ArgumentCaptor.forClass(List.class);
    verify(courseRepository).deleteCoursesByCourseIds(eq(studentId), deleteIdsCaptor.capture());
    assertThat(deleteIdsCaptor.getValue()).containsExactly(deleteId);

    // keepId は update される
    verify(courseRepository).updateCourseSelective(argThat(c -> keepId.equals(c.getCourseId())));
  }

  // --------------------------------------------------------------------
  // 6) courseId==null（新規追加）→ insert + status upsert(PROVISIONAL)
  // --------------------------------------------------------------------
  @Test
  void patchStudent_courseId未指定は新規追加され_PROVISIONALがupsertされること() {
    // Arrange
    StudentCourseDto dto = new StudentCourseDto();
    dto.setCourseId(null); // ★新規
    dto.setCourseName("Java");
    dto.setStartDate(LocalDate.of(2025, 1, 1));
    dto.setEndDate(null);
    dto.setApplicationStatus(null); // 未指定 → PROVISIONAL

    StudentRegistrationRequest req = new StudentRegistrationRequest();
    req.setStudent(null);
    req.setCourses(List.of(dto));
    req.setAppendCourses(true);

    // converter が dtoList → entityList を返す
    StudentCourse entity = new StudentCourse();
    entity.setCourseId(null); // ★新規扱い
    entity.setCourseName("Java");
    entity.setApplicationStatus(null); // 未指定

    when(converter.toCourseEntities(eq(studentId), anyList())).thenReturn(List.of(entity));

    // Act
    service.patchStudent(studentId, req, studentIdString);

    // Assert: course は insertCourses で 1件追加される
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<StudentCourse>> coursesCaptor = ArgumentCaptor.forClass(List.class);
    verify(courseRepository, times(1)).insertCourses(coursesCaptor.capture());

    List<StudentCourse> inserted = coursesCaptor.getValue();
    assertThat(inserted).hasSize(1);
    assertThat(inserted.get(0).getStudentId()).isEqualTo(studentId);
    assertThat(inserted.get(0).getCourseId()).isNotNull();

    UUID actualCourseId = inserted.get(0).getCourseId();

    // Assert: status は PROVISIONAL で upsert (update -> insert)
    verify(statusRepository, times(1)).upsertStatus(any(UUID.class), eq(actualCourseId),
        eq("PROVISIONAL"));

    // updateStatusByCourseId/insertStatusは呼ばれない
    verify(statusRepository, never()).updateStatusByCourseId(anyString(), any(UUID.class));
    verify(statusRepository, never()).insertStatus(any(UUID.class), any(UUID.class), anyString());
  }

  // ---------------------------------------------------------------
  // 7) course + status → updateCourseSelective + upsertStatusを呼ぶ
  // ---------------------------------------------------------------
  @Test
  void patchStudent_course情報とstatusが両方ある場合はupdateCourseSelectiveとupsertStatusが両方呼ばれること() {
    // Arrange
    UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

    // 所有チェックを通すため existingIds を用意
    StudentCourse existing = new StudentCourse();
    existing.setCourseId(courseId);
    when(courseRepository.findCoursesByStudentId(studentId)).thenReturn(List.of(existing));

    // DTO（courseFields + status）
    StudentCourseDto dto = new StudentCourseDto();
    dto.setCourseId(courseId.toString());
    dto.setCourseName("Java");
    dto.setApplicationStatus("IN_PROGRESS");
    dto.setStartDate(LocalDate.of(2025, 1, 1));

    StudentRegistrationRequest req = new StudentRegistrationRequest();
    req.setStudent(null);
    req.setCourses(List.of(dto));
    req.setAppendCourses(true);

    StudentCourse entity = new StudentCourse();
    entity.setCourseId(courseId);
    entity.setCourseName("Java"); // hasCourseFields=true
    entity.setStartDate(LocalDate.of(2025, 1, 1));
    entity.setEndDate(null);
    entity.setApplicationStatus("IN_PROGRESS"); // hasStatus=true

    when(converter.toCourseEntities(eq(studentId), anyList())).thenReturn(List.of(entity));

    when(courseRepository.updateCourseSelective(any(StudentCourse.class))).thenReturn(1);

    // Act
    service.patchStudent(studentId, req, studentIdString);

    // Assert
    verify(courseRepository).updateCourseSelective(argThat
        (c -> courseId.equals(c.getCourseId())));
    verify(statusRepository, times(1))
        .upsertStatus(any(UUID.class), eq(courseId), eq("IN_PROGRESS"));
    verify(statusRepository, never())
        .updateStatusByCourseId(anyString(), any(UUID.class));
    verify(statusRepository, never())
        .insertStatus(any(UUID.class), any(UUID.class), anyString());
  }

  // ------------------------------------------------------------------
  // 8) statusだけ更新（コース項目なし）→ updateCourseSelectiveは呼ばれない
  // ------------------------------------------------------------------
  @Test
  void patchStudent_statusだけ更新ならupdateCourseSelectiveせずupsertStatusだけすること() {
    // Arrange
    UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

    StudentCourseDto dto = new StudentCourseDto();
    dto.setCourseId(courseId.toString());
    dto.setCourseName("   "); // NotBlank制約があるので、PATCHでこれを送ると本来バリデーション対象
    // Service単体ではDTO前提なので、ここでは courseName 触らない想定として nullにするのが無難
    dto.setCourseName(null);
    dto.setApplicationStatus("IN_PROGRESS"); // ★statusだけ更新

    StudentRegistrationRequest req = new StudentRegistrationRequest();
    req.setStudent(null);
    req.setCourses(List.of(dto));
    req.setAppendCourses(true);

    // 既存コースに同じIDがある（所有OK）
    StudentCourse existing = new StudentCourse();
    existing.setCourseId(courseId);
    when(courseRepository.findCoursesByStudentId(studentId)).thenReturn(List.of(existing));

    // converter が返す entity：course fieldsは全部null、statusだけあり
    StudentCourse entity = new StudentCourse();
    entity.setCourseId(courseId);
    entity.setCourseName(null);
    entity.setStartDate(null);
    entity.setEndDate(null);
    entity.setApplicationStatus("IN_PROGRESS");

    when(converter.toCourseEntities(eq(studentId), anyList())).thenReturn(List.of(entity));

    // Act
    service.patchStudent(studentId, req, studentIdString);

    // Assert
    verify(courseRepository, never()).updateCourseSelective(any());
    verify(statusRepository, times(1))
        .upsertStatus(any(UUID.class), eq(courseId), eq("IN_PROGRESS")); // ←期待statusに合わせる
    verify(statusRepository, never())
        .updateStatusByCourseId(anyString(), any(UUID.class));
    verify(statusRepository, never())
        .insertStatus(any(UUID.class), any(UUID.class), anyString());
  }

  // --------------------------------------------------------------
  // 9) courseId!=null だが所有してない → ResourceNotFoundException
  // --------------------------------------------------------------
  @Test
  void patchStudent_courseIdが他人のものなら404_例外となること() {
    // Arrange
    UUID otherCourseId = UUID.fromString("123e4567-e89b-12d3-a456-426614174999");

    StudentCourseDto dto = new StudentCourseDto();
    dto.setCourseId(otherCourseId.toString());
    dto.setCourseName("AWS");

    StudentRegistrationRequest req = new StudentRegistrationRequest();
    req.setStudent(null);
    req.setCourses(List.of(dto));
    req.setAppendCourses(true);

    // 既存コースは別IDのみ（所有していない状態）
    StudentCourse existing = new StudentCourse();
    existing.setCourseId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
    when(courseRepository.findCoursesByStudentId(studentId)).thenReturn(List.of(existing));

    StudentCourse toUpdate = new StudentCourse();
    toUpdate.setCourseId(otherCourseId); // ★他人ID
    toUpdate.setCourseName("AWS"); // フィールドあり
    when(converter.toCourseEntities(eq(studentId), anyList())).thenReturn(List.of(toUpdate));

    // Act & Assert
    assertThatThrownBy(() -> service.patchStudent(studentId, req, studentIdString))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("courseId " + otherCourseId);

    verify(courseRepository, never()).updateCourseSelective(any());
    verifyNoInteractions(statusRepository);
  }

  // --------------------------------------------------------------
  // 10) courseId!=null かつ所有しているが、更新件数=0 → ResourceNotFoundException
  // --------------------------------------------------------------
  @Test
  void patchStudent_course更新でupdate0件ならResourceNotFoundExceptionとなること() {
    // Arrange
    UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

    // 所有チェック用の既存コース
    StudentCourse existing = new StudentCourse();
    existing.setCourseId(courseId);
    when(courseRepository.findCoursesByStudentId(studentId)).thenReturn(List.of(existing));

    // リクエスト（courseFieldsあり）
    StudentCourseDto dto = new StudentCourseDto();
    dto.setCourseId(courseId.toString());
    dto.setCourseName("Java");

    StudentRegistrationRequest req = new StudentRegistrationRequest();
    req.setStudent(null);
    req.setCourses(List.of(dto));
    req.setAppendCourses(true);

    StudentCourse toUpdate = new StudentCourse();
    toUpdate.setCourseId(courseId);
    toUpdate.setCourseName("Java"); // hasCourseFields=true
    when(converter.toCourseEntities(eq(studentId), anyList())).thenReturn(List.of(toUpdate));

    // ★更新0件 → not found 扱い
    when(courseRepository.updateCourseSelective(any(StudentCourse.class))).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> service.patchStudent(studentId, req, studentIdString))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("courseId " + courseId + " が見つかりません");

    verify(courseRepository).updateCourseSelective(any(StudentCourse.class));
    verifyNoInteractions(statusRepository); // update失敗で status 更新まで行かない想定
  }
}
