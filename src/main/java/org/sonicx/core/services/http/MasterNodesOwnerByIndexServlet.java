package org.sonicx.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.sonicx.api.GrpcAPI;
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
public class MasterNodesOwnerByIndexServlet extends HttpServlet {

    @Autowired
    private MasterNodeController masternodeController;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contract = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));
        JSONObject jsonObject = JSONObject.parseObject(contract);

        String owner = jsonObject.getString("address");
        byte[] contractAddress = masternodeController.getGenesisContractAddress();

        GrpcAPI.TransactionExtention result = masternodeController.mnOwnerIndexes(contractAddress, owner);

        if (result != null && result.getConstantResultCount() == 1) {
            response.getWriter().println(Util.printArgs(result.getConstantResult(0).toByteArray()));
            return;
        }

        response.getWriter().println("{}");
    }
}