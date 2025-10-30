package raisetech.student.management.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import raisetech.student.management.dto.StudentRegistrationRequest;

// Note: このエンドポイントは @Profile("test") のみで有効。
// 本番プロファイルではコンテナに登録されない（副作用防止）。
@Profile("test") // ← test プロファイルでのみ有効
@RestController
@RequestMapping("/api/students")
public class DebugStudentController {

  @PostMapping(path = "/debug-dto",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> debugDto(@RequestBody StudentRegistrationRequest req) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("studentNull", req.getStudent() == null);
    m.put("coursesNull", req.getCourses() == null);
    if (req.getStudent() != null) {
      m.put("fullName", req.getStudent().getFullName());
      m.put("gender", req.getStudent().getGender());
    }
    if (req.getCourses() != null && !req.getCourses().isEmpty()) {
      m.put("firstCourseName", req.getCourses().get(0).getCourseName());
      m.put("firstStartDate", String.valueOf(req.getCourses().get(0).getStartDate()));
    }
    return m;
  }

  /**
   * 一時デバッグ用: 生のJSONがどう届いているか確認
   */
  @PostMapping(path = "/debug-raw",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> debugRaw(@RequestBody Map<String, Object> raw) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("keys", raw.keySet());
    m.put("rawType", raw.getClass().getName());
    return m;
  }
}
