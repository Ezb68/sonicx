package org.sonicx.core.services.http;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;
import org.sonicx.common.crypto.ECKey;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.common.utils.Utils;
import org.sonicx.core.Wallet;


@Component
@Slf4j(topic = "API")
public class GenerateAddressServlet extends HttpServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      ECKey ecKey = new ECKey(Utils.getRandom());
      byte[] priKey = ecKey.getPrivKeyBytes();
      byte[] address = ecKey.getAddress();
      String priKeyStr = Hex.encodeHexString(priKey);
      String base58check = Wallet.encode58Check(address);
      String hexString = ByteArray.toHexString(address);
      JSONObject jsonAddress = new JSONObject();
      jsonAddress.put("address", base58check);
      jsonAddress.put("hexAddress", hexString);
      jsonAddress.put("privateKey", priKeyStr);
      response.getWriter().println(jsonAddress.toJSONString());
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
    doGet(request, response);
  }
}