package org.sonicx.core.services.interfaceOnSolidity.http.solidity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sonicx.core.services.http.GetTransactionInfoByIdServlet;
import org.sonicx.core.services.interfaceOnSolidity.WalletOnSolidity;


@Component
@Slf4j(topic = "API")
public class GetTransactionInfoByIdOnSolidityServlet
    extends GetTransactionInfoByIdServlet {

  @Autowired
  private WalletOnSolidity walletOnSolidity;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    walletOnSolidity.futureGet(() -> super.doGet(request, response));
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    walletOnSolidity.futureGet(() -> super.doPost(request, response));
  }
}
