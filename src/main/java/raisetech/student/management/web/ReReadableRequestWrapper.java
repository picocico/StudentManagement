package raisetech.student.management.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class ReReadableRequestWrapper extends HttpServletRequestWrapper {

  private final byte[] body;

  public ReReadableRequestWrapper(HttpServletRequest request, byte[] body) {
    super(request);
    this.body = (body != null) ? body : new byte[0];
  }

  @Override
  public ServletInputStream getInputStream() {
    final ByteArrayInputStream bais = new ByteArrayInputStream(body);
    return new ServletInputStream() {
      @Override
      public int read() {
        return bais.read();
      }

      @Override
      public boolean isFinished() {
        return bais.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        /* no-op */
      }
    };
  }

  public String getBodyAsText() {
    return new String(body, StandardCharsets.UTF_8);
  }

  public int length() {
    return body.length;
  }
}
