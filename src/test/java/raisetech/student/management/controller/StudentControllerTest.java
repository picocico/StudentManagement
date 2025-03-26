package raisetech.student.management.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.service.StudentService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudentControllerTest {

  private MockMvc mockMvc;

  @Mock
  private StudentService service;

  @InjectMocks
  private StudentController controller;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  public void testUpdateStudentWithValidationErrors() throws Exception {

    String studentId = "dadaf78f-c8c9-4725-a0a6-ed3bf31dd215";

    mockMvc.perform(post("/updateStudent")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("student.studentId", studentId)   // studentIdはstudentオブジェクトのフィールドとして送信
            .param("student.fullName", "")  // エラーを意図的に発生させる
            .param("student.furigana", "")  // エラーを意図的に発生させる
            .param("student.nickname", "")  // エラーを意図的に発生させる
            .param("student.email", "invalid-email")  // エラーを意図的に発生させる
            .param("student.gender", "")  // エラーを意図的に発生させる
            .accept(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isOk())  // エラーが発生しても200 OKが返る
        .andExpect(model().attributeExists("validationErrors"))  // エラーオブジェクトが存在するか
        .andExpect(view().name("editStudent"));  // エラー時に表示されるページ名
  }
}
















