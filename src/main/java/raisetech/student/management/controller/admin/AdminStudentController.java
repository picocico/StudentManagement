package raisetech.student.management.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import raisetech.student.management.exception.dto.ErrorResponse;
import raisetech.student.management.service.StudentService;
import raisetech.student.management.util.IdCodec;

/**
 * 管理者用の受講生物理削除 API コントローラー。
 *
 * <p>管理者権限を持つユーザーのみがアクセスでき、URL-safe Base64 形式で指定された
 * 受講生 ID（UUID/BINARY(16)）に対応するレコードをデータベースから完全に削除します。
 */
@Tag(name = "管理者用API", description = "管理者のみがアクセス可能な受講生操作API")
@SecurityRequirement(name = "basicAuth")
@RestController
@RequestMapping("/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

  private final StudentService studentService;
  private final IdCodec idCodec;

  /**
   * 受講生を物理削除します（管理者専用）。
   *
   * <p>処理の流れ:
   * <ul>
   *   <li>パス変数 {@code studentId}（URL-safe Base64 形式）を {@link IdCodec} で 16 バイト配列にデコードする</li>
   *   <li>デコードした ID を用いて {@link StudentService#forceDeleteStudent(byte[])} を呼び出し、該当レコードを物理削除する</li>
   * </ul>
   *
   * <p>正常終了時は HTTP 204 No Content を返します。
   * Base64 文字列が不正、または該当する受講生が存在しない場合は
   * {@link raisetech.student.management.exception.GlobalExceptionHandler} により
   * 400 / 404 のエラーレスポンスに変換されます。
   *
   * @param studentId URL-safe Base64 形式の受講生 ID （UUID/BINARY(16) を
   *                  {@code Base64.getUrlEncoder().withoutPadding()} でエンコードした文字列）
   * @return 削除成功時は本文なしの 204 No Content を返却します
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
    byte[] studentIdBytes = idCodec.decodeUuidBytesOrThrow(studentId);
    studentService.forceDeleteStudent(studentIdBytes);
    return ResponseEntity.noContent().build();
  }
}
