package raisetech.student.management.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import raisetech.student.management.controller.converter.StudentConverter;
import raisetech.student.management.service.StudentService;

/**
 * 管理者用の受講生物理削除APIコントローラー。
 * <p>
 * 管理者のみがアクセス可能で、物理削除を行います。
 */
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
  @DeleteMapping("/{studentId}")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')") // 管理者のみアクセス可能
  public ResponseEntity<Void> forceDeleteStudent(@PathVariable String studentId) {
    byte[] studentIdBytes = converter.decodeBase64(studentId);
    studentService.forceDeleteStudent(studentIdBytes);
    return ResponseEntity.noContent().build();
  }
}
