package com.rpccenter.registry;

import com.common.extension.SPI;
import com.rpccenter.remote.dto.RpcRequest;

import java.net.InetSocketAddress;

/**
 * @author yls91
 */
@SPI
public interface ServiceDiscovery {
    /** 请求服务时  传入RpcRequest对象寻找
     * @param rpcRequest RpcRequest对象
     * @return 响应服务器的地址
     */
    InetSocketAddress getIpByRequest(RpcRequest rpcRequest);
}
