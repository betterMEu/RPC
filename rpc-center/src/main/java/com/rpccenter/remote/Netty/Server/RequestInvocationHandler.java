package com.rpccenter.remote.Netty.Server;

import com.common.factory.SingletonFactory;
import com.rpccenter.remote.Netty.RpcException;
import com.rpccenter.remote.ServiceProvider.Impl.ServiceProviderImpl;
import com.rpccenter.remote.dto.RpcRequest;
import com.rpccenter.remote.ServiceProvider.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * 处理来自客户端的消息
 * @author yls91
 */
@Slf4j
public class RequestInvocationHandler {
    private final ServiceProvider serviceProvider;

    public RequestInvocationHandler() {
        serviceProvider = SingletonFactory.getInstance(ServiceProviderImpl.class);
    }

    /**
     * 处理请求 需要获取目标方法 跳转
     * */
    public Object handle(RpcRequest rpcRequest) {
        //根据请求里的服务名在本地寻找对应的服务
        Object service = serviceProvider.getService(rpcRequest.getServiceName());
        return invokeTargetMethod(rpcRequest,service);
    }

    /**
     * 获取目标方法
     * */
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(),rpcRequest.getParamTypes());
            result  = method.invoke(service,rpcRequest.getParameters());
            log.info("服务[{}]成功调用方法:[{}]",rpcRequest.getServiceName(),rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            throw new RpcException(e.getMessage(),e);
        }
        return result;
    }
}
