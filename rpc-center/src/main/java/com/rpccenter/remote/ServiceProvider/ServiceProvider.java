package com.rpccenter.remote.ServiceProvider;

import com.common.extension.SPI;
import com.rpccenter.remote.dto.RpcServiceConfig;

/**
 * @author yls91
 */
@SPI
public interface ServiceProvider {

    void addService(RpcServiceConfig rpcService);

    Object getService(String rpcServiceName);

    void publishService(RpcServiceConfig rpcService);

}
