package com.rpccenter.loadbalance;

import com.rpccenter.remote.dto.RpcRequest;

import java.util.List;

/**
 * @author yls91
 */
public abstract class AbstractLoadBalance implements LoadBalance{

    @Override
    public String theBestHost(List<String> hostList,RpcRequest rpcRequest) {
        if(hostList == null) {
            return null;
        }
        if(hostList.size() == 1) {
            return hostList.get(0);
        }
        return doSelect(hostList,rpcRequest);
    }

    protected abstract String doSelect(List<String> hostList, RpcRequest rpcRequest);


}
