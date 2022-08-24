package com.rpccenter.remote.Netty.Client;

import com.rpccenter.remote.dto.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yls91
 */
public class UnprocessedRequests {
    private static Map<String, CompletableFuture<RpcResponse<Object>>> UNPROCESSED_RPC_REQUEST = new ConcurrentHashMap<>();

    public void put(String requestId,CompletableFuture<RpcResponse<Object>> future) {
        UNPROCESSED_RPC_REQUEST.put(requestId,future);
    }

    public void complete(RpcResponse<Object> rpcResponse) {
        CompletableFuture<RpcResponse<Object>> future = UNPROCESSED_RPC_REQUEST.remove(rpcResponse.getId());
        if (null != future) {
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }

}
