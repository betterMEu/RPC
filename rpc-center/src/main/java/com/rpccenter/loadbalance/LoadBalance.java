package com.rpccenter.loadbalance;

import com.common.extension.SPI;
import com.rpccenter.remote.dto.RpcRequest;

import java.util.List;

/**
 * @author yls91
 */
@SPI
public interface LoadBalance {

    /**
     * 选择最佳地址
     *
     * @param hostList 服务器地址集合
     * @return 最佳地址
     */
    String theBestHost(List<String> hostList, RpcRequest rpcRequest);
}
