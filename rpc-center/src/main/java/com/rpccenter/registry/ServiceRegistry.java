package com.rpccenter.registry;


import com.common.extension.SPI;

import java.net.InetSocketAddress;

/**
 * @author yls91
 */
@SPI
public interface ServiceRegistry {

    /** 注册服务 只需要定义好服务名称 和 服务器地址
     * @param ServiceName 服务名
     * @param inetSocketAddress 地址
     */
    void registerService(String ServiceName, InetSocketAddress inetSocketAddress);
}
