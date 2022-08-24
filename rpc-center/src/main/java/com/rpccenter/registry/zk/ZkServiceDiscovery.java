package com.rpccenter.registry.zk;

import com.common.enums.RpcErrorMessageEnum;
import com.common.extension.ExtensionLoader;
import com.rpccenter.loadbalance.LoadBalance;
import com.rpccenter.registry.ServiceDiscovery;
import com.rpccenter.remote.Netty.RpcException;
import com.rpccenter.remote.dto.RpcRequest;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author yls91
 */
public class ZkServiceDiscovery implements ServiceDiscovery {

    private final LoadBalance loadBalance;

    public ZkServiceDiscovery() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("balance");
    }

    @Override
    public InetSocketAddress getIpByRequest(RpcRequest rpcRequest) {
        String serviceName = rpcRequest.getServiceName();
        CuratorFramework zkClient = CuratorUtil.getZkClient();

        List<String> serviceList = CuratorUtil.getServiceIP(zkClient,serviceName);
        if(serviceList == null || serviceList.isEmpty()) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND,serviceName);
        }

        //选择服务器地址  返回的形式：“127.0.0.1：8080”
        String address = loadBalance.theBestHost(serviceList,rpcRequest);

        // ip 端口 分离
        String[] str = address.split(":");
        String ip = str[0];
        int port = Integer.parseInt(str[1]);

        return new InetSocketAddress(ip,port);
    }
}
