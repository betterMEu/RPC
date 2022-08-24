package com.rpccenter.remote.dto;

import lombok.*;

/**
 * @author yls91
 * 客户端请求包括 接口名+方法名+方法参数+方法参数类型+版本+组别+请求Id
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcRequest {

    private String id;
    private String interfaceName;
    private String version;
    private String group;

    /**
     * 参数个数 、参数类型  用于反射获取具体的方法
     * */
    private Class<?>[] paramTypes;

    private String methodName;

    /**
     * 参数值
     */
    private Object[] parameters;

    /**
     * 用于 服务器 获取服务名称 获得具体请求并处理
     * */
    public String getServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}
