package org.sonicx.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.sonicx.api.GrpcAPI.TransactionExtention;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.core.mastrnode.MasterNodeController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;


@Component
@Slf4j(topic = "API")
public class MasterNodesActivateMasterNodeServlet extends HttpServlet {

    @Autowired
    private MasterNodeController masternodeController;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        byte[] contractAddress;
        byte[] callerAddress;
        long feeLimit;
        long callValue;

        String contract = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));
        JSONObject jsonObject = JSONObject.parseObject(contract);

        contractAddress = masternodeController.getGenesisContractAddress();
        callerAddress = ByteArray.fromHexString(jsonObject.getString("caller_address"));
        feeLimit = jsonObject.getLong("fee_limit");

        TransactionExtention result = masternodeController.activateMasternode(contractAddress, callerAddress,
                feeLimit);
        response.getWriter().println(Util.printTransactionExtention(result));
    }
}