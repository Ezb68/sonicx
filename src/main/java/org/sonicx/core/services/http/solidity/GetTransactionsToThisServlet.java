package org.sonicx.core.services.http.solidity;

import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sonicx.api.GrpcAPI.AccountPaginated;
import org.sonicx.api.GrpcAPI.TransactionList;
import org.sonicx.core.WalletSolidity;
import org.sonicx.core.services.http.JsonFormat;
import org.sonicx.core.services.http.Util;


@Component
@Slf4j(topic = "API")
public class GetTransactionsToThisServlet extends HttpServlet {

  @Autowired
  private WalletSolidity walletSolidity;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    try {
      String input = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);
      AccountPaginated.Builder builder = AccountPaginated.newBuilder();
      JsonFormat.merge(input, builder, visible);
      AccountPaginated accountPaginated = builder.build();
      ByteString toAddress = accountPaginated.getAccount().getAddress();
      long offset = accountPaginated.getOffset();
      long limit = accountPaginated.getLimit();
      if (toAddress != null && offset >= 0 && limit >= 0) {
        TransactionList list = walletSolidity.getTransactionsToThis(toAddress, offset, limit);
        resp.getWriter().println(Util.printTransactionList(list, visible));
      } else {
        resp.getWriter().print("{}");
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        resp.getWriter().println(e.getMessage());
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {

  }
}
