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
import org.sonicx.api.GrpcAPI;
import org.sonicx.api.GrpcAPI.EasyTransferMessage;
import org.sonicx.api.GrpcAPI.EasyTransferResponse;
import org.sonicx.api.GrpcAPI.Return.response_code;
import org.sonicx.common.crypto.ECKey;
import org.sonicx.core.Wallet;
import org.sonicx.core.capsule.TransactionCapsule;
import org.sonicx.core.exception.ContractValidateException;
import org.sonicx.core.services.http.JsonFormat.ParseException;
import org.sonicx.protos.Contract.TransferContract;
import org.sonicx.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j(topic = "API")
public class EasyTransferServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    GrpcAPI.Return.Builder returnBuilder = GrpcAPI.Return.newBuilder();
    EasyTransferResponse.Builder responseBuild = EasyTransferResponse.newBuilder();
    boolean visible = false;
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      visible = Util.getVisiblePost(input);
      EasyTransferMessage.Builder build = EasyTransferMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      byte[] privateKey = wallet.pass2Key(build.getPassPhrase().toByteArray());
      ECKey ecKey = ECKey.fromPrivate(privateKey);
      byte[] owner = ecKey.getAddress();
      TransferContract.Builder builder = TransferContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(owner));
      builder.setToAddress(build.getToAddress());
      builder.setAmount(build.getAmount());

      TransactionCapsule transactionCapsule;
      transactionCapsule = wallet
          .createTransactionCapsule(builder.build(), ContractType.TransferContract);
      transactionCapsule.sign(privateKey);
      GrpcAPI.Return retur = wallet.broadcastTransaction(transactionCapsule.getInstance());
      responseBuild.setTransaction(transactionCapsule.getInstance());
      responseBuild.setResult(retur);
      response.getWriter().println(Util.printEasyTransferResponse(responseBuild.build(), visible));
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
    } catch (IOException e) {
      logger.debug("IOException: {}", e.getMessage());
    } catch (ContractValidateException e) {
      returnBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getMessage()));
      responseBuild.setResult(returnBuilder.build());
      try {
        response.getWriter().println(JsonFormat.printToString(responseBuild.build(), visible));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
      return;
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
    }
  }
}
