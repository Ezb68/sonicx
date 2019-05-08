package org.sonicx.core.services.http;

import lombok.extern.slf4j.Slf4j;
import org.sonicx.api.GrpcAPI;
import org.sonicx.core.mastrnode.MasterNodeController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j(topic = "API")
public class MasterNodesWholeActivatedCollateralServlet extends HttpServlet {

    @Autowired
    private MasterNodeController masternodeController;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        byte[] contractAddress = masternodeController.getGenesisContractAddress();
        GrpcAPI.TransactionExtention result = masternodeController.wholeActivatedCollateral(contractAddress);

        if (result != null && result.getConstantResultCount() == 1) {
            response.getWriter().println(Util.printArgs(result.getConstantResult(0).toByteArray()));
            return;
        }
        response.getWriter().println("{}");
    }
}