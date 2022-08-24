package com.rpccenter.remote.Netty.Client;

import com.common.extension.SPI;
import com.rpccenter.remote.dto.RpcRequest;
import com.rpccenter.remote.dto.RpcResponse;

import java.util.concurrent.CompletableFuture;


/**
 * @author yls91
 */
@SPI
public interface RpcRequestTransport {

    /**
     * 客户端继承 主要功能用于传送请求。客户端有很多种实现，如：Socket ，Netty
     * @param rpcRequest 请求内容
     * @return res 请求返回的结果
     */
    CompletableFuture<RpcResponse<Object>> sendRpcRequest(RpcRequest rpcRequest);

}

