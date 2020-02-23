/*
 * Copyright (c) [2019] SONICX
 *
 * It has been added for MasterNode
 */

package org.sonicx.core.config.args;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "app")
public class MasterNode {

    @Getter
    @Setter
    private boolean enable;

    @Getter
    @Setter
    private String byteCode;

    @Getter
    @Setter
    private String abi;

    @Getter
    @Setter
    private long minBlocksBeforeActivation;

    @Getter
    @Setter
    private String operatorPrivateKey;

    @Getter
    @Setter
    private long rewardsPeriod;

    @Getter
    @Setter
    private  long minimumCollateral;

    public MasterNode() {
    }
}
