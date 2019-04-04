package org.sonicx.core.services.http;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sonicx.api.GrpcAPI.BytesMessage;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.core.Wallet;
import org.sonicx.protos.Protocol.DelegatedResourceAccountIndex;

@Component
@Slf4j(topic = "API")
public class GetDelegatedResourceAccountIndexServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      String address = request.getParameter("value");
      DelegatedResourceAccountIndex reply =
          wallet.getDelegatedResourceAccountIndex(
              ByteString.copyFrom(ByteArray.fromHexString(address)));
      if (reply != null) {
        response.getWriter().println(JsonFormat.printToString(reply));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build);
      DelegatedResourceAccountIndex reply =
          wallet.getDelegatedResourceAccountIndex(build.getValue());
      if (reply != null) {
        response.getWriter().println(JsonFormat.printToString(reply));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }
}
