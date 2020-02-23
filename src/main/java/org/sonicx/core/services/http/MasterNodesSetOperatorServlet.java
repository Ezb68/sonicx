package org.sonicx.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.sonicx.api.GrpcAPI;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.core.mastrnode.MasterNodeController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "API")
public class MasterNodesSetOperatorServlet extends HttpServlet {

    @Autowired
    private MasterNodeController masternodeController;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        byte[] contractAddress;
        byte[] callerAddress;
        long feeLimit;
        String operator;
        String operatorRewardAddress;
        long operatorRewardRatio;

        String contract = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));
        JSONObject jsonObject = JSONObject.parseObject(contract);

        contractAddress = masternodeController.getGenesisContractAddress();
        callerAddress = ByteArray.fromHexString(jsonObject.getString("caller_address"));
        feeLimit = jsonObject.getLong("fee_limit");
        operator = jsonObject.getString("operator");
        operatorRewardAddress = jsonObject.getString("operator_reward_address");
        operatorRewardRatio = jsonObject.getLong("operator_reward_ratio");

        GrpcAPI.TransactionExtention result = masternodeController.setOperator(contractAddress, callerAddress,
                feeLimit, operator, operatorRewardAddress, new BigInteger(String.valueOf(operatorRewardRatio)));
        response.getWriter().println(Util.printTransactionExtention(result, false));
    }
}