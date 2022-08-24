package com.rpccenter.remote.dto;

import lombok.*;

import java.io.Serializable;

/**
 * @author yls91
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcResponse<T> implements Serializable {

    /**
     * 匹配RpcRequest的id 每个request都有对应的response
     * */
    private String id;

    private Integer requestCode;
    private String request;

    private T result;

    public static <T> RpcResponse<T> success(T data, String requestId) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setRequestCode(RpcResponseCodeEnum.SUCCESS.getCode());
        response.setRequest(RpcResponseCodeEnum.SUCCESS.getMessage());
        response.setId(requestId);
        response.setResult(data);
        return response;
    }

    public static <T> RpcResponse<T> fail() {
        RpcResponse<T> response = new RpcResponse<>();
        response.setRequestCode(RpcResponseCodeEnum.FAIL.getCode());
        response.setRequest(RpcResponseCodeEnum.FAIL.getMessage());
        return response;
    }
}
