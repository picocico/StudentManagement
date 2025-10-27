package raisetech.student.management.controller;

import java.time.LocalDate;
import java.util.List;
import raisetech.student.management.dto.StudentCourseDto;
import raisetech.student.management.dto.StudentDto;
import raisetech.student.management.dto.StudentRegistrationRequest;

final class TestRequests {

  static StudentRegistrationRequest validRegistrationRequest() {
    var dto = new StudentDto(
        null, "テスト　花子", "てすと　はなこ", "ハナちゃん",
        "test@example.com", "大阪", 30, "FEMALE", "コース追加予定", false);
    var course = new StudentCourseDto(
        null, "Javaコース",
        LocalDate.parse("2024-03-01"),
        LocalDate.parse("2024-09-30"));
    var req = new StudentRegistrationRequest();  // ← デフォルトコンストラクタ
    req.setStudent(dto);
    req.setCourses(List.of(course));
    req.setDeleted(false);
    req.setAppendCourses(false);
    return req;
  }

  // static StudentRegistrationRequest invalidRegistrationRequest() {
  // 既存の“エラーを出すための入力”をここに集約
  // var dto = new StudentDto(null, null, null, null,"bad", null, -1, null, null, false);
  // var req = new StudentRegistrationRequest();  // ← デフォルトコンストラクタ
  // req.setStudent(dto);
  // req.setCourses(List.of());// or Collections.emptyList()
  // 「courses 未指定（null）」の異常を試すなら、ここは設定しない
  // ただし @NotNull なので、これは「null のときはバリデーションエラー」を期待するテストになります。
  // req.setDeleted(false);
  // appendCourses は未指定（null）で「デフォルト false 扱い」を検証してもOK
  // return req;}

  private TestRequests() {
  }
}
