package com.rpccenter.remote.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author yls91
 */

@AllArgsConstructor
@Getter
@ToString
public enum RpcResponseCodeEnum {
    //
    SUCCESS(200, "successful"),
    FAIL(500, "failure");
    private final int code;
    private final String message;

}
