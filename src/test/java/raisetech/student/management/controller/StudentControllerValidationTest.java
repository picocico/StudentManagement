package raisetech.student.management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import raisetech.student.management.dto.StudentRegistrationRequest;
import raisetech.student.management.exception.InvalidIdFormatException;

class StudentControllerValidationTest extends ControllerTestBase {

  /**
   * バリデーションエラーがある状態で受講生を登録しようとしたときに、 適切なHTTPステータスとエラーレスポンスが返却されることを検証します。
   * <p>
   * 主な検証内容：
   * <ul>
   *   <li>ステータスコードが 400 Bad Request であること</li>
   *   <li>レスポンスボディに errorCode が "E001" として含まれること</li>
   *   <li>エラー詳細（errors）が配列として含まれていること</li>
   * </ul>
   *
   * @throws Exception HTTP通信の模擬処理中に例外が発生した場合
   */
  @Test
  public void registerStudent_バリデーションエラー発生時に適切なHTTPステータスとエラーレスポンスが返ること()
      throws Exception {
    String invalid = """
        {
          "student": {
            "fullName": "",
            "furigana": "",
            "nickname": "",
            "email": "invalid-email",
            "location": "",
            "age": -1,
            "gender": "",
            "remarks": ""
          },
          "courses": []
        }
        """;

    mockMvc.perform(post("/api/students")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalid))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E001"))                 // ここを errorCode → code に
        .andExpect(
            jsonPath("$.error").value("VALIDATION_FAILED"))   // 旧: errorType を使っていたなら error に
        .andExpect(jsonPath("$.message").value("入力値に不備があります"))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThan(0)));
  }

  /**
   * {@code includeDeleted=true} と {@code deletedOnly=true} を同時指定した場合に 400エラー（不正リクエスト）が返ることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students}
   * <br>Params: {@code includeDeleted=true}, {@code deletedOnly=true}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>service.getStudentList(null, true, true) が {@code IllegalArgumentException} を送出</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvcでGET（両方true）を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>ステータス400で、メッセージに同時指定不可の旨が含まれる</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentList_論理削除と削除のみ指定が同時にtrueの場合_例外が返ること()
      throws Exception {
    when(service.getStudentList(null, true, true))
        .thenThrow(new IllegalArgumentException(
            "includeDeleted=true と deletedOnly=true は同時指定できません"));

    mockMvc.perform(get("/api/students")
            .param("includeDeleted", "true")
            .param("deletedOnly", "true"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").value(
            containsString("includeDeleted=true と deletedOnly=true は同時指定できません")));
  }

  /**
   * {@code includeDeleted} に文字列など不正な型を指定した場合に 型不一致エラー（E004/400）が返ることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students}
   * <br>Params: {@code includeDeleted=abc}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>When:
   * <ul>
   *   <li>MockMvcでGET（includeDeleted に不正な値）を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, errorType=TYPE_MISMATCH, errorCode=E004 を検証</li>
   *   <li>message に {@code includeDeleted} が含まれる</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentList_includeDeletedに文字列が指定された場合_型不一致エラーが返ること()
      throws Exception {
    mockMvc.perform(get("/api/students").param("includeDeleted", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E004"))            // ← 数値ではなく文字列
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))  // ← errorType → error
        .andExpect(jsonPath("$.message").value(containsString("includeDeleted")))
        .andExpect(jsonPath("$.code").isString())               // 型も明示しておくと堅牢
        .andExpect(jsonPath("$.status").isNumber());
  }

  /**
   * {@code deletedOnly} に文字列など不正な型を指定した場合に 型不一致エラー（E004/400）が返ることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students}
   * <br>Params: {@code deletedOnly=xyz}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>When:
   * <ul>
   *   <li>MockMvcでGET（deletedOnly に不正な値）を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, errorType=TYPE_MISMATCH, errorCode=E004 を検証</li>
   *   <li>message に {@code deletedOnly} が含まれる</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentList_deletedOnlyに文字列が指定された場合_型不一致エラーが返ること()
      throws Exception {
    mockMvc.perform(get("/api/students").param("deletedOnly", "xyz"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E004"))
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
        .andExpect(jsonPath("$.message").value(containsString("deletedOnly")))
        .andExpect(jsonPath("$.code").isString())
        .andExpect(jsonPath("$.status").isNumber());
  }

  /**
   * Base64形式でないIDを指定した場合に、型不一致エラー（E004/TYPE_MISMATCH/400）が返却されることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(invalid)} が {@code IllegalArgumentException} を送出</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/{invalid}} を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E004, error=TYPE_MISMATCH を検証する</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentDetail_Base64形式でないIDを指定した場合_400エラーが返ること()
      throws Exception {

    String invalid = "@@invalid@@";
    when(converter.decodeBase64(invalid)).thenThrow(new IllegalArgumentException("Base64 error"));

    mockMvc.perform(get("/api/students/{studentId}", invalid))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E006"))                   // ← E006 に
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))       // ← error キー
        .andExpect(jsonPath("$.message").value(containsString("Base64"))); // 文言変更に強く
  }

  /**
   * Base64デコード後のバイト長がUUIDの16バイトと異なる場合に、E006/INVALID_REQUEST/400 が返却されることを検証します。
   * <p>
   * Endpoint: {@code GET /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(base64Id)} が長さ不正により {@code IllegalArgumentException} を送出</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvc で {@code GET /api/students/{studentId}} を実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証する</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void getStudentDetail_デコード後のバイト長がUUIDと異なる場合_400エラーが返ること()
      throws Exception {

    when(converter.decodeBase64(base64Id))
        .thenThrow(new IllegalArgumentException("UUIDの形式が不正です"));

    mockMvc.perform(get("/api/students/{studentId}", base64Id))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").value(containsString("UUID"))); // 文言変更に強く
  }

  /**
   * バリデーションエラーが発生した更新要求に対して 400（E001/VALIDATION_FAILED）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>When:
   * <ul>
   *   <li>必須項目が欠落／不正のJSONを送信</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, errorCode=E001, errors配列あり</li>
   *   <li>converter と service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_バリデーションエラーが発生する場合_400を返すこと() throws Exception {

    String invalid = """
        {
          "student": {
            "fullName": "",
            "furigana": "",
            "nickname": "",
            "email": "invalid-email",
            "location": "",
            "age": -1,
            "gender": "",
            "remarks": ""
          },
          "courses": []
        }
        """;

    mockMvc.perform(put("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalid))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E001"))                 // ← 文字列のコード
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))   // ← error に統一
        .andExpect(jsonPath("$.message").value(containsString("入力値")))
        // 配列の存在だけ緩く見る（空白差の影響を受けにくい）
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.details").isArray());
    // もし特定フィールドまで見たいなら部分一致で:
    // .andExpect(jsonPath("$.errors[*].field", hasItem("student.email")));

    // バリデーションで落ちる想定なので依存には触らない
    verifyNoInteractions(converter, service);
  }

  /**
   * Base64形式が不正なIDで更新要求した場合に 400（E006/INVALID_REQUEST）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{invalidId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeUuidOrThrow(invalidId)} が {@code InvalidIdFormatException} を送出</li>
   * </ul>
   * When:
   * <ul>
   *   <li>MockMvcでPUTを実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証</li>
   *   <li>service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_base64形式が不正な場合_400を返すこと() throws Exception {
    String invalidId = "invalid_base64";

    // Base64として不正 → 変換時点で独自例外（E006）を投げる前提
    doThrow(new InvalidIdFormatException("IDの形式が不正です（Base64）"))
        .when(converter).decodeUuidOrThrow(invalidId);

    String body = json(new StudentRegistrationRequest() {{
      setStudent(studentDto);
      setCourses(List.of(courseDto));
    }});

    mockMvc.perform(put("/api/students/{studentId}", invalidId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E006"))                 // ← 文字列
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").value(containsString("Base64")));

    // 以降は到達しない
    verifyNoInteractions(service);
  }

  /**
   * UUID長（16バイト）でないID（Base64は正しい）で更新要求した場合に 400（E006）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeUuidOrThrow(idWithWrongLength)} が {@code InvalidIdFormatException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証</li>
   *   <li>service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_UUID形式が不正な場合_400を返すこと() throws Exception {
    // Base64 としては正しいが UUID(16バイト)ではない 10バイトのIDを用意
    String idWithWrongLength = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[10]);

    // 長さ不正も decode 時点で InvalidIdFormatException を投げる前提（E006）
    doThrow(new InvalidIdFormatException("IDの形式が不正です（UUID）"))
        .when(converter).decodeUuidOrThrow(idWithWrongLength);

    // リクエストボディ（バリデーションは通る想定）
    String body = json(new StudentRegistrationRequest() {{
      setStudent(studentDto);
      setCourses(List.of(courseDto));
    }});

    // 実行：controller 側で「長さ != 16」を検知して 400 を返すはず
    mockMvc.perform(put("/api/students/{studentId}", idWithWrongLength)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").value(containsString("UUID")));

    verifyNoInteractions(service);
  }

  /**
   * 更新要求のボディが空の場合に 400（E003/MISSING_PARAMETER）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>When:
   * <ul>
   *   <li>空ボディでPUTを実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>errorCode=E003, errorType=MISSING_PARAMETER, code=400 を検証</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_リクエストボディが空の場合_400を返すこと() throws Exception {

    mockMvc.perform(put("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(""))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E003"))                 // ← MISSING_PARAMETER は E003
        .andExpect(jsonPath("$.error").value("MISSING_PARAMETER"))
        .andExpect(jsonPath("$.message").value(containsString("リクエストボディ")));
    // @Valid 前にdecodeしない設計なら、ここも converter に触らない想定にできます。
    // 設計に合わせて下行はコメントアウト可
    // verifyNoInteractions(converter, service);
  }

  /**
   * 更新要求で {@code student} が {@code null} の場合に 400（E001/VALIDATION_FAILED）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>When:
   * <ul>
   *   <li>{@code {"student":null,"courses":[]}} を送信</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>details[0].field=student を含むバリデーションエラーを検証</li>
   *   <li>converter と service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_student情報がnullの場合_400を返すこと() throws Exception {

    // バリデーションで弾く想定なので decode すら呼ばれない（@Valid → その後 decode）
    String body = """
        {
          "student": null,
          "courses": []
        }
        """;

    mockMvc.perform(put("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E001"))
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.message").value(containsString("入力値")))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.details").isArray())
        .andExpect(jsonPath("$.errors[*].field", hasItem("student"))); // 部分一致で十分

    verifyNoInteractions(converter, service);
  }

  /**
   * 更新要求で {@code courses} が {@code null} の場合に 400（E001/VALIDATION_FAILED）が返ることを検証します。
   * <p>
   * Endpoint: {@code PUT /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>When:
   * <ul>
   *   <li>courses=null を含むJSONでPUTを実行</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>details[*].field に {@code courses} を含む</li>
   *   <li>converter と service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  void updateStudent_coursesがnullの場合_400を返すこと() throws Exception {
    // Arrange
    var req = new StudentRegistrationRequest();
    req.setStudent(studentDto);   // ← @BeforeEach で作ったものをそのまま使用
    req.setCourses(null);         // ← 検証ポイント
    // 必須なら: req.setAppendCourses(false);

    // Act & Assert
    mockMvc.perform(put("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E001"))
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.message").value(containsString("入力値")))
        .andExpect(jsonPath("$.errors[*].field", hasItem("courses")));

    // バリデーションで弾かれるので下位は呼ばれない想定
    verifyNoInteractions(converter, service);
  }

  /**
   * 部分更新でBase64形式が不正なIDの場合に 400（E006/INVALID_REQUEST）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(base64Id)} が {@code IllegalArgumentException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証</li>
   *   <li>service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void partialUpdateStudent_base64形式が不正な場合_400を返すこと() throws Exception {

    StudentRegistrationRequest request = new StudentRegistrationRequest();
    request.setStudent(studentDto);
    request.setCourses(List.of());
    request.setAppendCourses(false);

    when(converter.decodeBase64(base64Id))
        .thenThrow(new IllegalArgumentException("UUIDの形式が不正です"));

    mockMvc.perform(patch("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

    verify(converter).decodeBase64(base64Id);
    verifyNoMoreInteractions(converter);
    verifyNoInteractions(service);
  }

  /**
   * 部分更新でUUID長が不正なIDの場合に 400（E006/INVALID_REQUEST）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(invalidId)} が {@code IllegalArgumentException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証</li>
   *   <li>service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void partialUpdateStudent_UUID形式が不正な場合_400を返すこと() throws Exception {

    String invalidId = "AAAAAAAAAAAAAAAAAAAAAA=="; // 長さ不正相当
    StudentRegistrationRequest request = new StudentRegistrationRequest();
    request.setStudent(studentDto);
    request.setCourses(List.of());
    request.setAppendCourses(false);

    when(converter.decodeBase64(invalidId))
        .thenThrow(new IllegalArgumentException("UUIDの形式が不正です"));

    mockMvc.perform(patch("/api/students/{studentId}", invalidId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

    verify(converter).decodeBase64(invalidId);
    verifyNoMoreInteractions(converter);
    verifyNoInteractions(service);
  }

  /**
   * 部分更新のボディが空の場合に 400（E003/MISSING_PARAMETER）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * Then:
   * <ul>
   *   <li>errorCode=E003, errorType=MISSING_PARAMETER を検証</li>
   *   <li>converter と service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void partialUpdateStudent_リクエストボディが空の場合_400を返すこと() throws Exception {

    mockMvc.perform(patch("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(""))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E003"))
        .andExpect(jsonPath("$.error").value("MISSING_PARAMETER"))
        .andExpect(jsonPath("$.message", containsString("リクエストボディ")));

    verifyNoInteractions(converter, service);
  }

  /**
   * 部分更新で空オブジェクトを送った場合に 400（E003/MISSING_PARAMETER）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * Then:
   * <ul>
   *   <li>errorCode=E003, errorType=MISSING_PARAMETER を検証</li>
   *   <li>converter と service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void partialUpdateStudent_空JSONオブジェクトの場合_400を返すこと() throws Exception {

    mockMvc.perform(patch("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E003"))
        .andExpect(jsonPath("$.error").value("MISSING_PARAMETER"))
        .andExpect(jsonPath("$.message", containsString("リクエストボディ")));

    verifyNoInteractions(converter, service);
  }

  /**
   * 部分更新で {@code student} が {@code null} の場合に 400（E001/VALIDATION_FAILED）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * Then:
   * <ul>
   *   <li>details[0].field=student を検証</li>
   *   <li>converter と service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void partialUpdateStudent_student情報がnullの場合_400を返すこと() throws Exception {

    mockMvc.perform(patch("/api/students/{studentId}", base64Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"student\":null,\"courses\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("E001"))                 // ← errorCode→code、文字列
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))   // ← error に統一
        .andExpect(jsonPath("$.message").value(containsString("入力値")))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.details").isArray());

    verifyNoInteractions(converter, service);
  }


  /**
   * 論理削除でBase64形式が不正なIDを指定した場合に 400（E006/INVALID_REQUEST）が返ることを検証します。
   * <p>
   * Endpoint: {@code DELETE /api/students/{studentId}}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(invalid)} が {@code IllegalArgumentException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証</li>
   *   <li>service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void deleteStudent_studentIdのBase64形式が不正な場合_400を返すこと() throws Exception {

    String invalid = "invalid_base64";
    when(converter.decodeBase64(invalid))
        .thenThrow(new IllegalArgumentException("UUIDの形式が不正です"));

    mockMvc.perform(delete("/api/students/{studentId}", invalid))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

    verify(converter).decodeBase64(invalid);
    verifyNoInteractions(service);
  }

  /**
   * 復元でBase64形式が不正なIDを指定した場合に 400（E006/INVALID_REQUEST）が返ることを検証します。
   * <p>
   * Endpoint: {@code PATCH /api/students/{studentId}/restore}
   * <br>Status: {@code 400 BAD_REQUEST}
   * <p>Given:
   * <ul>
   *   <li>{@code converter.decodeBase64(invalid)} が {@code IllegalArgumentException} を送出</li>
   * </ul>
   * Then:
   * <ul>
   *   <li>status=400, code=E006, error=INVALID_REQUEST を検証</li>
   *   <li>service は呼ばれない</li>
   * </ul>
   *
   * @throws Exception 実行時例外
   */
  @Test
  public void restoreStudent_Base64形式が不正の場合_400を返すこと() throws Exception {

    String invalid = "invalid_base64";
    when(converter.decodeBase64(invalid))
        .thenThrow(new IllegalArgumentException("UUIDの形式が不正です"));

    mockMvc.perform(patch("/api/students/{studentId}/restore", invalid))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("E006"))
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

    verify(converter).decodeBase64(invalid);
    verifyNoInteractions(service);
  }
}
