package com.rpccenter.remote.ServiceProvider.Impl;

import com.common.enums.RpcErrorMessageEnum;
import com.common.extension.ExtensionLoader;
import com.rpccenter.registry.ServiceRegistry;
import com.rpccenter.remote.Netty.RpcException;
import com.rpccenter.remote.Netty.Server.NettyServer;
import com.rpccenter.remote.dto.RpcServiceConfig;
import com.rpccenter.remote.ServiceProvider.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yls91
 */
@Slf4j
public class ServiceProviderImpl implements ServiceProvider {

    private final Map<String, Object> serviceMap;

    private final ServiceRegistry serviceRegistry;

    public ServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension("zk");
    }

    @Override
    public void addService(RpcServiceConfig rpcService) {
        //service.FirstServicegroup11.0
        String serviceName = rpcService.getServiceName();
        if(serviceMap.containsKey(serviceName)) {
            return;
        }
        serviceMap.put(serviceName,rpcService.getService());
        log.info("添加服务成功: [{}] 类：[{}] 接口:{}",serviceName,rpcService.getService().getClass(),rpcService.getService().getClass().getInterfaces());
    }

    @Override
    public Object getService(String serviceName) {
        //service.AddServiceThree1.0
        Object service = serviceMap.get(serviceName);
        if(service == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    /**
     * 单机 无法做到多服务器多IP  都获取本机ip
     * */
    @Override
    public void publishService(RpcServiceConfig rpcService) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();

            addService(rpcService);

            serviceRegistry.registerService(rpcService.getServiceName(), new InetSocketAddress(host, NettyServer.PORT));
        } catch (UnknownHostException e) {
            log.error("获取本地主机ip发生错误", e);
        }
    }
}
