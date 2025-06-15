package raisetech.student.management.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import raisetech.student.management.exception.dto.ErrorResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 認証に失敗したリクエストに対して、JSON形式で401エラーを返すエントリポイント。
 */
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException
  )
      throws IOException {

    ErrorResponse errorResponse = new ErrorResponse(
        401,
        HttpStatus.UNAUTHORIZED.value(),
        "UNAUTHORIZED",
        "E401",
        "認証に失敗しました。ログインが必要です。",
        null
    );

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
