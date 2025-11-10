package raisetech.student.management.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RawBodyCaptureFilter extends org.springframework.web.filter.OncePerRequestFilter {

  public static final String ATTR_RAW_BODY_LEN = "RAW_BODY_LENGTH";
  public static final String ATTR_RAW_BODY_STATE = "RAW_BODY_STATE";

  public enum RawBodyState {
    NONE,
    EMPTY_OBJECT,
    NON_EMPTY
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    // 元のストリームを全部吸ってバイト配列化
    byte[] bytes = request.getInputStream().readAllBytes();
    String text = new String(bytes, StandardCharsets.UTF_8).trim();

    // 状態を判定
    RawBodyState state =
        (bytes.length == 0 || text.isEmpty()) ? RawBodyState.NONE
            : "{}".equals(text) ? RawBodyState.EMPTY_OBJECT
                : RawBodyState.NON_EMPTY;

    // 属性に格納（コントローラで参照）
    request.setAttribute(ATTR_RAW_BODY_LEN, bytes.length);
    request.setAttribute(ATTR_RAW_BODY_STATE, state);

    // 以降の処理のために“再読可能”なラッパで包んで流す
    chain.doFilter(new ReReadableRequestWrapper(request, bytes), response);
  }
}
