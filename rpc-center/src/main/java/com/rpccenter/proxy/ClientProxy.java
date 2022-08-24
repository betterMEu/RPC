package com.rpccenter.proxy;

import com.common.enums.RpcErrorMessageEnum;
import com.rpccenter.remote.Netty.Client.NettyClient;
import com.rpccenter.remote.Netty.RpcException;
import com.rpccenter.remote.Netty.Client.RpcRequestTransport;
import com.rpccenter.remote.dto.RpcServiceConfig;
import com.rpccenter.remote.dto.RpcRequest;
import com.rpccenter.remote.dto.RpcResponse;
import com.rpccenter.remote.dto.RpcResponseCodeEnum;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author yls91
 */
public class ClientProxy implements InvocationHandler {

    private final RpcRequestTransport rpcRequestTransport;
    private final RpcServiceConfig rpcService;

    public ClientProxy(RpcRequestTransport rpcRequestTransport, RpcServiceConfig rpcService) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcService = rpcService;
    }

    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest rpcRequest = RpcRequest.builder()
                .id(UUID.randomUUID().toString())
                .methodName(method.getName())
                .paramTypes(method.getParameterTypes())
                .parameters(args)
                .interfaceName(method.getDeclaringClass().getName())
                .group(rpcService.getGroup())
                .version(rpcService.getVersion())
                .build();

        RpcResponse<Object> rpcResponse = null;
        if (rpcRequestTransport instanceof NettyClient) {
            CompletableFuture<RpcResponse<Object>> completableFuture = rpcRequestTransport.sendRpcRequest(rpcRequest);
            rpcResponse = completableFuture.get();
        }
        this.check(rpcResponse, rpcRequest);
        return rpcResponse.getResult();
    }

    private void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, "interface:" + rpcRequest.getInterfaceName());
        }

        if (!rpcRequest.getId().equals(rpcResponse.getId())) {
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, "interface:" + rpcRequest.getInterfaceName());
        }

        if (rpcResponse.getRequestCode() == null || !rpcResponse.getRequestCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, "interface:" + rpcRequest.getInterfaceName());
        }
    }

}
