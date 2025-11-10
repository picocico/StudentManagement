package raisetech.student.management.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import raisetech.student.management.exception.dto.ErrorResponse;

/**
 * Spring Securityの認可（Authorization）失敗時に呼び出されるカスタムハンドラ。
 *
 * <p>必要な権限を持たないリソースへのアクセスが拒否された場合（HTTP 403 Forbidden）に、 クライアントへJSON形式のエラーレスポンスを返却します。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

  /**
   * エラーレスポンスオブジェクトをJSON文字列にシリアライズするためのObjectMapper。 Lombokの{@code @RequiredArgsConstructor}
   * により、コンストラクタインジェクションされます。
   */
  private final ObjectMapper objectMapper;

  /**
   * 認可が拒否された場合に実行される処理。
   *
   * <p>HTTPステータス403 (Forbidden) と、詳細なエラー情報を含むJSONボディをレスポンスとして返します。
   *
   * @param request 認可が拒否されたリクエスト
   * @param response クライアントへのレスポンス
   * @param ex 発生した {@link AccessDeniedException}
   * @throws IOException レスポンスへの書き込み中にI/Oエラーが発生した場合
   */
  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    // クライアントに返却するエラーレスポンスのボディを生成
    var body =
        ErrorResponse.of(
            HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "E403", "アクセスが拒否されました。管理者権限が必要です。");

    // レスポンスのステータス、コンテントタイプ、エンコーディングを設定
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    // ObjectMapperを使用してレスポンスボディにJSONを書き込む
    objectMapper.writeValue(response.getWriter(), body);
  }
}
