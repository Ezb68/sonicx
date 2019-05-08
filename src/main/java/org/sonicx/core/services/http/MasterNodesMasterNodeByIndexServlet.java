package org.sonicx.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.sonicx.common.runtime.vm.DataWord;
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
public class MasterNodesMasterNodeByIndexServlet extends HttpServlet {

    @Autowired
    private MasterNodeController masternodeController;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        byte[] contractAddress = masternodeController.getGenesisContractAddress();
        long index;

        String contract = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));
        JSONObject jsonObject = JSONObject.parseObject(contract);

        index = jsonObject.getLong("index");

        MasterNodeController.MasternodesArrayResults result = masternodeController.masternodesArray(contractAddress,
                new BigInteger(new DataWord(index).getData()));

        if (result != null) {
            response.getWriter().println(Util.printArgs(result.OwnerAuthAddress, result.OperatorAuthAddress,
                    result.OwnerRewardAddress, result.OperatorRewardAddress, result.CollateralAmount.toByteArray(),
                    result.OperatorRewardRatio.toByteArray(), result.AnnouncementBlockNumber.toByteArray(),
                    result.MinActivationBlockNumber.toByteArray(), result.ActivationBlockNumber.toByteArray(),
                    result.PrevRewardBlockNumber.toByteArray()));
            return;
        }
        response.getWriter().println("{}");
    }
}