package raisetech.student.management.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.exception.dto.ErrorResponse;
import raisetech.student.management.service.StudentService;

/**
 * 管理者用の受講生物理削除APIコントローラー。
 *
 * <p>管理者のみがアクセス可能で、物理削除を行います。
 */
@Tag(name = "管理者用API", description = "管理者のみがアクセス可能な受講生操作API")
@SecurityRequirement(name = "basicAuth")
@RestController
@RequestMapping("/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

  private final StudentService studentService;
  private final StudentConverter converter;

  /**
   * 受講生を物理削除します（管理者専用）。
   *
   * @param studentId 物理削除対象の受講生ID（UUIDをBINARY(16)型で格納した16バイトの配列）
   * @return 204 No Content
   */
  @Operation(
      summary = "受講生の物理削除（管理者専用）",
      description = "Base64形式のUUIDで指定された受講生をデータベースから完全に削除します。",
      parameters = @Parameter(name = "studentId", description = "Base64形式の受講生ID", required = true),
      responses = {
        @ApiResponse(responseCode = "204", description = "削除成功"),
        @ApiResponse(
            responseCode = "401",
            description = "認証失敗（未ログイン）",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "認証エラーまたは権限不足",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "対象の受講生が存在しない",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @DeleteMapping("/{studentId}")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')") // 管理者のみアクセス可能
  public ResponseEntity<Void> forceDeleteStudent(@PathVariable String studentId) {
    byte[] studentIdBytes = converter.decodeBase64(studentId);
    studentService.forceDeleteStudent(studentIdBytes);
    return ResponseEntity.noContent().build();
  }
}
