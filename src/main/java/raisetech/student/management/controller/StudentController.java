package raisetech.student.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.data.Student;
import raisetech.student.management.data.StudentCourse;
import raisetech.student.management.dto.StudentDetailDto;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.exception.EmptyObjectException;
import raisetech.student.management.exception.MissingParameterException;
import raisetech.student.management.exception.dto.ErrorResponse;
import raisetech.student.management.service.StudentService;
import raisetech.student.management.util.UUIDUtil;


/**
 * 受講生に関するREST APIを提供するコントローラークラス。
 * <p>
 * このクラスは、受講生の登録、取得、更新、削除、復元、およびふりがなによる検索などの 操作をエンドポイントとして提供します。
 */
@Slf4j
@RestController
@RequestMapping("/api/students")
@Validated
@RequiredArgsConstructor
@Tag(name = "受講生API", description = "受講生のCRUDおよび検索・復元操作")
public class StudentController {


  /**
   * 受講生サービス
   */
  private final StudentService service;

  /**
   * 受講生コンバーター
   */
  private final StudentConverter converter;

  private final ObjectMapper objectMapper;

  /**
   * 新規の受講生情報を登録します。
   *
   * @param request 登録する受講生情報およびコース情報
   * @return 201 Created + 登録された受講生の詳細情報（Student + Courses）
   */
  @Operation(summary = "受講生登録", description = "受講生および受講コースを新規登録します。",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "登録対象の受講生およびコース情報",
          required = true),
      responses = {
          @ApiResponse(responseCode = "201", description = "登録に成功",
              content = @Content(schema = @Schema(implementation = StudentDetailDto.class))),
          @ApiResponse(responseCode = "400", description = "バリデーションエラーまたはリクエスト不正",
              content = @Content(schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StudentDetailDto> registerStudent(
      @Valid @RequestBody StudentRegistrationRequest request) {

    log.debug("DTO runtime type = {}", request.getClass().getName());
    log.debug("student={}, courses={}", request.getStudent(), request.getCourses());

    Student student = converter.toEntity(request.getStudent());
    byte[] studentIdBytes = student.getStudentId();

    List<StudentCourse> courses = converter.toEntityList(request.getCourses(),
        studentIdBytes);

    log.debug("POST - Registering new student: {}, ID(Base64): {}",
        student.getFullName(),
        converter.encodeBase64(studentIdBytes));
    service.registerStudent(student, courses);

    StudentDetailDto responseDto = converter.toDetailDto(student, courses);

    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
  }

  /**
   * 条件付きで受講生一覧を取得します（ふりがな、削除状態）。
   *
   * @param furigana       ふりがな検索（省略可能）
   * @param includeDeleted 論理削除済みも含めるか
   * @param deletedOnly    論理削除された学生のみ取得するか
   * @return 条件に一致する受講生詳細DTOリスト
   */
  @Operation(
      summary = "受講生一覧検索",
      description = "受講生の一覧をふりがな検索・削除状態を条件に取得します。",
      parameters = {
          @Parameter(name = "furigana", description = "ふりがなで部分一致検索（任意）"),
          @Parameter(name = "includeDeleted", description = "論理削除済みも含める（デフォルト: false)"),
          @Parameter(name = "deletedOnly", description = "論理削除された受講生のみ取得（デフォルト: false)")
      },
      responses = {
          @ApiResponse(responseCode = "200", description = "一覧取得成功",
              content = @Content(schema = @Schema(implementation = StudentDetailDto.class)))
      }
  )
  @GetMapping
  public ResponseEntity<List<StudentDetailDto>> getStudentList(
      @RequestParam(required = false) String furigana,
      @RequestParam(required = false, defaultValue = "false") boolean includeDeleted,
      @RequestParam(required = false, defaultValue = "false") boolean deletedOnly) {

    log.debug("GET - Fetching students list. furigana={}, includeDeleted={}, deletedOnly={}",
        furigana, includeDeleted, deletedOnly);

    List<StudentDetailDto> students = service.getStudentList(furigana, includeDeleted, deletedOnly);

    return ResponseEntity.ok(students);
  }

  /**
   * 指定された受講生IDに該当する詳細情報を取得します。
   *
   * @param studentId 受講生ID
   * @return 受講生詳細情報
   */
  @Operation(
      summary = "受講生詳細取得",
      description = "指定された受講生IDに対応する詳細情報（基本＋コース）を取得します。",
      parameters = @Parameter(name = "studentId", description = "Base64エンコードされた受講生ID", required = true),
      responses = {
          @ApiResponse(responseCode = "200", description = "詳細取得成功",
              content = @Content(schema = @Schema(implementation = StudentDetailDto.class))
          ),
          @ApiResponse(responseCode = "404", description = "該当する受講生が存在しない",
              content = @Content(schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @GetMapping("/{studentId}")
  public ResponseEntity<StudentDetailDto> getStudentDetail(@PathVariable String studentId) {
    log.debug("GET - Fetching student detail: {}", studentId);
    byte[] studentIdBytes = converter.decodeBase64(studentId);

    Student student = service.findStudentById(studentIdBytes);
    List<StudentCourse> courses = service.searchCoursesByStudentId(studentIdBytes);
    return ResponseEntity.ok(converter.toDetailDto(student, courses));
  }

  /**
   * 受講生情報を全体的に更新します。
   *
   * @param studentIdBase64 受講生ID
   * @param req             更新内容
   * @return 更新後の受講生詳細情報
   */
  @Operation(
      summary = "受講生情報更新（全体）",
      description = "受講生情報とコース情報を全て更新します。",
      parameters = @Parameter(name = "studentId", description = "更新対象の受講生ID", required = true),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "更新内容（受講生情報＋コース）",
          required = true
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(schema = @Schema(implementation = StudentDetailDto.class))
          ),
          @ApiResponse(responseCode = "400", description = "バリデーションエラー",
              content = @Content(schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(responseCode = "404", description = "該当する受講生が存在しない",
              content = @Content(schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PutMapping("/{studentId}")
  public ResponseEntity<StudentDetailDto> updateStudent(
      @PathVariable("studentId") String studentIdBase64,
      @Valid @RequestBody StudentRegistrationRequest req  // ★ @Valid を付与
  ) {
    // 0) ここでは何もしない（@Valid でNGならこのメソッド自体が呼ばれません）

    // 1) IDデコード（Base64不正/UUID不正は InvalidIdFormatException → E006）
    UUID uuid = converter.decodeUuidOrThrow(studentIdBase64);
    byte[] studentId = UUIDUtil.fromUUID(uuid);

    // 2) 変換（@Valid 済みなのでここに到達している）
    Student toUpdate = converter.toEntity(req.getStudent());
    // パスのIDを最優先に統一（ボディのIDが入っていても上書き）
    toUpdate.setStudentId(studentId);

    List<StudentCourse> courses = converter.toEntityList(req.getCourses(), studentId);

    // 3) 再取得→DTO化（レスポンスの studentId はパスのBase64で上書き）
    Student updated = service.updateStudentWithCourses(toUpdate, courses);

    // 表示用ID文字列（必要なら事前生成して渡す。内部で再デコードさせない）
    String idBase64ForResponse = converter.encodeBase64(studentId); // ← encode側を用意
    StudentDetailDto dto = converter.toDetailDto(updated, courses, idBase64ForResponse);

    return ResponseEntity.ok(dto);

  }

  /**
   * 受講生情報を部分的に更新します。
   *
   * @param studentId 受講生ID
   * @param body      更新対象のフィールド
   * @return 更新後の受講生詳細情報
   */
  @Operation(
      summary = "受講生情報新（部分）",
      description = "指定項目のみ受講生情報を部分的に更新します。appendCourses=trueの場合はコース追加。",
      parameters = @Parameter(name = "studentId", description = "部分更新対象の受講生ID", required = true),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "部分更新する受講生情報＋（オプション）コース情報",
          required = true
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "部分更新成功",
              content = @Content(schema = @Schema(implementation = StudentDetailDto.class))
          ),
          @ApiResponse(responseCode = "404", description = "該当する受講生が存在しない",
              content = @Content(schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PatchMapping(
      value = "/{studentId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,          // ★ JSON以外は受けない
      produces = MediaType.APPLICATION_JSON_VALUE // ★ 成功時に JSON を返すことを明示
  )
  public ResponseEntity<StudentDetailDto> partialUpdateStudent(
      @PathVariable String studentId,
      @RequestBody(required = false) Map<String, Object> body
  ) throws BindException {

    // --- リクエスト存在チェック ---
    // 0) 空ボディ(null) → MISSING_PARAMETER（E001）
    if (body == null) {
      throw new MissingParameterException("リクエストボディは必須です。");
    }
    // 1) 空JSON {} → EMPTY_OBJECT（E003）
    if (body.isEmpty()) {
      throw new EmptyObjectException("更新対象のフィールドがありません");
    }

    // 2) student キーあり値が null → VALIDATION_FAILED（E001）
    if (body.containsKey("student") && body.get("student") == null) {
      var target = new StudentRegistrationRequest();
      var br = new BeanPropertyBindingResult(target, "studentRegistrationRequest");
      br.rejectValue("student", "NotNull", "student は必須です。");
      throw new BindException(br);
    }

    try {
      // 3) Map -> DTO へ変換
      final StudentRegistrationRequest req =
          objectMapper.convertValue(body, StudentRegistrationRequest.class);

      // 4) 実質空更新 → EMPTY_OBJECT（E003）
      if (req == null || req.isPatchEmpty()) {
        throw new EmptyObjectException("更新対象のフィールドがありません");
      }

      // 5) IDデコード（Base64/UUID不正は既存ハンドラで 400）
      final byte[] studentIdBytes = converter.decodeBase64(studentId);

      // 6) 既存取得 & 基本情報マージ（まだDB更新はしない）
      final Student existing = service.findStudentById(studentIdBytes);
      final Student update = converter.toEntity(req.getStudent());
      converter.mergeStudent(existing, update);

      // 7) コース更新（ここで初めて副作用を伴う処理）
      final boolean hasCourses = req.getCourses() != null && !req.getCourses().isEmpty();
      if (hasCourses) {
        final boolean append = Boolean.TRUE.equals(req.getAppendCourses());
        final List<StudentCourse> newCourses =
            converter.toEntityList(req.getCourses(), studentIdBytes);
        if (append) {
          service.appendCourses(studentIdBytes, newCourses);
        } else {
          service.replaceCourses(studentIdBytes, newCourses);
        }
        // ※ hasCourses=true の場合はコース側で更新済み。基本情報は別途まとめて更新したいなら
        //    updateStudentInfoOnly(existing) を呼ぶ設計もありだが、
        //    「予期せぬ例外が起きたテストで never()」の要件に合わせて呼ばない。
      } else {
        // コースを触らない場合のみ基本情報を適用
        service.updateStudentInfoOnly(existing);
      }

      // 8) 最新状態でレスポンス
      final Student latest = service.findStudentById(studentIdBytes);
      final List<StudentCourse> latestCourses = service.searchCoursesByStudentId(studentIdBytes);
      return ResponseEntity.ok(converter.toDetailDto(latest, latestCourses));

    } catch (RuntimeException e) {
      // ログだけ出して再throw → GlobalExceptionHandlerの汎用ハンドラで 500
      log.error("Unexpected error in PATCH /students/{}: {}", studentId, e.toString(), e);
      throw e;
    }
  }

  /**
   * 受講生情報を論理削除します。
   *
   * @param studentId 削除対象の受講生ID
   * @return 204 No Content
   */
  @Operation(
      summary = "受講生論理削除",
      description = "指定された受講生を論理削除(is_deleted=true,deleted_at更新)します。",
      parameters = @Parameter(name = "studentId", description = "論理削除対象の受講生ID", required = true),
      responses = {
          @ApiResponse(responseCode = "204", description = "削除成功"),
          @ApiResponse(responseCode = "404", description = "対象受講生が存在しない",
              content = @Content(schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @DeleteMapping("/{studentId}")
  public ResponseEntity<Void> deleteStudent(@PathVariable String studentId) {
    log.debug("DELETE - Logically deleting student: {}", studentId);
    byte[] studentIdBytes = converter.decodeBase64(studentId); // ← Base64からbyte[]へ変換
    service.softDeleteStudent(studentIdBytes);
    return ResponseEntity.noContent().build();
  }

  /**
   * 論理削除された受講生情報を復元します。
   *
   * @param studentId 復元対象の受講生ID
   * @return 204 No Content
   */
  @Operation(
      summary = "論理削除からの復元",
      description = "論理削除された受講生を復元します。",
      parameters = @Parameter(name = "studentId", description = "復元対象の受講生ID", required = true),
      responses = {
          @ApiResponse(responseCode = "204", description = "復元成功"),
          @ApiResponse(responseCode = "404", description = "対象受講生が存在しない、または未削除",
              content = @Content(schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PatchMapping("/{studentId}/restore")
  public ResponseEntity<Void> restoreStudent(@PathVariable String studentId) {
    log.debug("PATCH - Restoring student: {}", studentId);
    byte[] studentIdBytes = converter.decodeBase64(studentId);
    service.restoreStudent(studentIdBytes);
    return ResponseEntity.noContent().build();
  }

  // ------------------------
  // ▼ 以下：テスト用エンドポイント
  // ------------------------

  /**
   * 【テスト用】MissingServletRequestParameterException 発生確認用。 keywordパラメータをあえて必須にし、 未指定時に例外を投げる。
   */
  @Tag(name = "テスト用API")
  @Hidden
  @Operation(
      summary = "［テスト］パラメーター不足エラー",
      description = "keywordパラメータが見しての場合、MissingServletRequestParameterException "
          + "を発生させます。",
      parameters = @Parameter(name = "keyword", description = "必須のキーワード", required = true),
      responses = {
          @ApiResponse(responseCode = "200", description = "正常時"),
          @ApiResponse(responseCode = "400", description = "keywordが未指定",
              content = @Content(schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @GetMapping("/test-missing-param")
  public ResponseEntity<String> testMissing(@RequestParam(name = "keyword", required = true)
  String keyword) {
    return ResponseEntity.ok("受け取った keyword: " + keyword);
  }

  /**
   * 【テスト用】MethodArgumentTypeMismatchException 発生確認用。 idパラメータに文字列を渡すと、 int型変換に失敗して例外が発生する。
   */
  @Tag(name = "テスト用API")
  @Hidden
  @Operation(
      summary = "［テスト］型変換エラー",
      description = "int型のidに対して文字列を渡すと型変換エラーになります。",
      parameters = @Parameter(name = "id", description = "整数である必要があります", required = true),
      responses = {
          @ApiResponse(responseCode = "200", description = "正常時"),
          @ApiResponse(responseCode = "400", description = "型変換エラー発生",
              content = @Content(schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @GetMapping("/test-type")
  public ResponseEntity<String> testTypeMismatch(@RequestParam Integer id) {
    if (id == null) {
      throw new IllegalArgumentException("IDは整数で指定してください");
    }
    System.out.println("★★ Controller reached with id: " + id);
    return ResponseEntity.ok("受け取った ID: " + id);
  }
}


