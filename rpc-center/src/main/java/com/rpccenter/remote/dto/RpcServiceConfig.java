package com.rpccenter.remote.dto;

import lombok.*;

/**
 * server向zk注册服务时
 * @author yls91
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcServiceConfig {
    private String version;
    private String group;
    private Object service;

    //service.FirstServicegroup11.0
    public String getServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }

    //service.FirstService
    public String getInterfaceName() {
        return this.service.getClass().getInterfaces()[0].getCanonicalName();
    }
}
